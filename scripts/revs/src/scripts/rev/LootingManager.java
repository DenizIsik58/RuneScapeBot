package scripts.rev;


import org.tribot.script.sdk.*;
import org.tribot.script.sdk.pricing.Pricing;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Area;
import org.tribot.script.sdk.types.GroundItem;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import scripts.api.MyScriptVariables;
import scripts.api.utility.MathUtility;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class LootingManager {

    private static AtomicBoolean startLooting = new AtomicBoolean(false);

    private static final String[] lootToPickUp = new String[]{
            "Looting bag", "Bracelet of ethereum (uncharged)", "Battlestaff", "Rune full helm",
            "Rune platebody", "Rune platelegs", "Rune kiteshield", "Rune warhammer", "Dragon dagger",
            "Dragon longsword", "Dragon platelegs", "Dragon plateskirt", "Dragon med helm", "Coal",
            "Adamantite bar", "Runite ore", "Black dragonhide", "Yew logs", "Runite bar", "Mahogany plank",
            "Magic logs", "Uncut dragonstone", "Yew seed", "Magic seed", "Amulet of avarice", "Craw's bow (u)",
            "Thammaron's sceptre (u)", "Viggora's chainmace (u)", "Ancient emblem", "Ancient totem", "Ancient crystal",
            "Ancient statuette", "Ancient medallion", "Ancient effigy", "Ancient relic", "Dragonstone bolt tips",
            "Death rune", "Blood rune", "Blighted super restore(4)", "Onyx bolt tips", "Law rune", "Ring of wealth",
    };

    private static final Area southOrk = Area.fromRectangle(new WorldTile(3200, 10106, 0), new WorldTile(3232, 10086, 0));
    private static final Area goblin = Area.fromRectangle(new WorldTile(3234, 10110, 0), new WorldTile(3251, 10082, 0));

    private static int tripValue = 0;
    private static int totalValue = 0;

    public static void loot() {
        if (!startLooting.get()) {
            startLooting.set(true);


            startLooting.set(false);

            if (!Combat.isInWilderness()) {
                Log.debug("Not in wilderness. Cancelling looting");
                if (!MyRevsClient.getScript().isState(State.BANKING)){
                    MyRevsClient.getScript().isState(State.BANKING);
                }
                return;
            }

            if (MyRevsClient.getScript().isState(State.BANKING) || !MyRevsClient.getScript().isState(State.LOOTING)) {
                Log.debug("It's banking state! Cannot loot");
                return;
            }

            Log.debug("Started looting process");

            List<GroundItem> possibleLoot = getAllLoot();


            for (int itemIndex = 0; itemIndex < possibleLoot.size(); itemIndex++) {
                if (hasPkerBeenDetected()) {
                    return;
                }

                var item = possibleLoot.get(itemIndex);

                openLootingBag();

                var countBeforePickingUp = getAllLoot().size();


                // TODO: If loot value is over X amount don't tele. Try to take it no matter what.
                item.interact("Take", () -> hasPkerBeenDetected() || !Combat.isInWilderness());

            /*if (itemIndex == 0) {
                Log.debug("First item to pick up. Hovering over teleport in case pker is waiting.");
                Equipment.Slot.RING.getItem().ifPresent(ring -> {
                    ring.hoverMenu("Grand Exchange");

                });
            }*/

                if (hasPkerBeenDetected()) {
                    Log.debug("Pker has been detected. Cancelled further looting");
                    return;
                }

                var changed = Waiting.waitUntil(4000, () -> hasDecreased(countBeforePickingUp));

                if (!changed) {
                    Log.debug("Not changed");
                    loot();
                } else {

                    if (Pricing.lookupPrice(item.getId()).orElse(0) * item.getStack() >= 450000) {
                        try {
                            var outputFile = ScreenShotManager.takeScreenShotAndSave("drops");

                            MyRevsClient.getScript().getLootWebhook().setUsername("Revenant Farm")
                                    .setContent("@everyone **" + MyPlayer.getUsername() + " - Revs** - " + "You have received a drop - **" + item.getName() + " - Value = " + (Pricing.lookupPrice(item.getId()).orElse(0) * item.getStack()) + "**")
                                    .addFile(outputFile)
                                    .execute();
                        } catch (Exception e) {
                            Log.error(e);
                        }
                    }
                    tripValue += Pricing.lookupPrice(item.getId()).orElse(0) * item.getStack();
                    totalValue += Pricing.lookupPrice(item.getId()).orElse(0) * item.getStack();
                    var totalString = MathUtility.getProfitPerHourString(totalValue);
                    MyScriptVariables.setProfit(totalString);
                }
            }

            if (getTripValue() >= 200000) {

                    TeleportManager.teleportOut();


                try {
                    var outputFile = ScreenShotManager.takeScreenShotAndSave("success");

                    MyRevsClient.getScript().getSuccessfullTripHook().setUsername("Revenant Farm")
                            .setContent("**" + MyPlayer.getUsername() + " - Revs** - " + "Successful trip - **" + " - Value = " + LootingManager.getTripValue() + "**")
                            .addFile(outputFile)
                            .execute();

                } catch (Exception e) {
                    Log.error(e);
                }

                MyRevsClient.getScript().setState(State.BANKING);
                Log.debug("Switched to banking stage. I hit 200k+ bag");
                return;
            }

            // starts back here with brea
            var allPossibleFood = getAllFood();

            for (int itemIndex = 0; itemIndex < allPossibleFood.size(); itemIndex++) {
                if (Inventory.getAll().size() < 27 && Inventory.contains("Looting bag") && !getAllFood().isEmpty()) {
                    var foodCount = Query.inventory().actionEquals("Eat").count();
                    closeLootingBag();
                    Log.debug("My inventory is not full, I have a looting bag, and there are angler or manta on the floor");
                    var food = allPossibleFood.get(itemIndex);
                    food.interact("Take", () -> hasPkerBeenDetected() || !Combat.isInWilderness());

                    var pickedUp = Waiting.waitUntil(4000, () -> foodCount == Query.inventory().actionEquals("Eat").count() + 1);

                    if (hasPkerBeenDetected()) {
                        Log.debug("Pker has been detected. Cancelled further looting");
                        return;
                    }

                    if (!pickedUp) {
                        loot();
                    }
                } else {
                    break;
                }
            }
            Log.debug("I'm done looting");
            if (Combat.isInWilderness() && MyRevsClient.myPlayerIsInCave()) {
                MyRevsClient.getScript().getSelectedMonsterTile().clickOnMinimap();
                MyRevsClient.getScript().setState(State.KILLING);
            }

            if (RevkillerManager.getTarget() != null && RevkillerManager.getTarget().isValid()) {

                if (!RevkillerManager.getTarget().isVisible()) {
                    RevkillerManager.getTarget().adjustCameraTo();
                }
                RevkillerManager.getTarget().click();
            }

            Log.debug("Ended looting process. Switching back to killing");
            if (!Combat.isInWilderness()) {
                Log.debug("I'm not in wildy switching to bank");
                MyRevsClient.getScript().setState(State.BANKING);
            }
        }
    }

    private static void closeLootingBag(){
        getLootingBag().filter(lootingBag -> lootingBag.getId() == 22586).ifPresent(lb -> {
            Log.debug("Closing looting bag");
            Waiting.waitUntil(() -> lb.click("Close"));
            Waiting.waitUntil(500, () -> Inventory.contains(11941));
        });

    }

    public static void openLootingBag() {
        getLootingBag().ifPresent(lootingBag -> {
            if (lootingBag.getId() == 11941) {
                lootingBag.click("Open");
                Waiting.waitUntil(500, () -> Inventory.contains(22586));
            }
        });

        if (Inventory.isFull() && Query.inventory().actionEquals("Eat").isAny()) {
            Query.inventory().actionEquals("Eat").findClosestToMouse().ifPresent(food -> food.click("Eat"));
            Waiting.waitNormal(1000,150);
        }
    }

    private static Optional<InventoryItem> getLootingBag() {
        return Query.inventory().nameEquals("Looting bag").findFirst();
    }

    private static List<GroundItem> getAllLoot() {
        return Query.groundItems()
                .nameEquals(lootToPickUp)
                .inArea(southOrk)
                .sorted(Comparator.comparingInt(item -> Pricing.lookupPrice(item.getId()).orElse(0) * item.getStack()))
                .toList();
    }

    private static List<GroundItem> getAllFood(){
            return Query.groundItems()
                    .inArea(southOrk)
                    .nameEquals("Blighted manta ray", "Blighted anglerfish")
                    .toList();

    }

    public static boolean hasPkerBeenDetected() {
        if (MyRevsClient.getScript().getPlayerDetectionThread() != null){
            return MyRevsClient.getScript().getPlayerDetectionThread().hasPkerBeenDetected();
        }
       return false;
    }

    public static boolean hasDecreased(int count) {
        return getAllLoot().size() == count - 1 || getAllLoot().size() == 0;
    }

    public static boolean hasLootBeenDetected() {
        if (hasPkerBeenDetected()) {
            Log.debug("Pker has been detected. inside loot has been detected");
            return false;
        }


        return !getAllLoot().isEmpty();
//         for (var item : lootToPickUp) {
//             if (Query.groundItems().nameEquals(item).isAny()) {
//                 return true;
//             }
//         }
//         return false;
    }

    public static int getTotalValue() {
        return totalValue;
    }

    public static void setTotalValue(int totalValue) {
        LootingManager.totalValue = totalValue;
    }

    public static String[] getLootToPickUp() {
        return lootToPickUp;
    }

    public static int getTripValue() {
        return tripValue;
    }

    public static void resetTripValue() {
        LootingManager.tripValue = 0;
    }
}
