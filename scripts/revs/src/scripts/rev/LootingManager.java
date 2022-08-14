package scripts.rev;


import org.tribot.script.sdk.*;
import org.tribot.script.sdk.pricing.Pricing;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.GroundItem;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import scripts.api.MyScriptVariables;
import scripts.api.utility.MathUtility;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class LootingManager {

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
    private static int tripValue = 0;
    private static int totalValue = 0;

    public static void loot() {
        Log.debug("Started looting process");

        List<GroundItem> possibleLoot = getAllLoot();


        for (int itemIndex = 0; itemIndex < possibleLoot.size(); itemIndex++) {
            if (hasPkerBeenDetected()) {
                return;
            }

            Query.npcs().nameEquals("Revenant maledictus").findFirst().ifPresent(boss -> {
                if (boss.isValid() || boss.isAnimating() || boss.isMoving() || boss.isHealthBarVisible() || boss.getTile().isVisible() || boss.getTile().isRendered()){
                    TeleportManager.teleportOutOfWilderness("Boss has been seen! Trying to teleport out");
                    MyRevsClient.getScript().setState(State.BANKING);
                }
            });


            var item = possibleLoot.get(itemIndex);

                openLootingBag();

            var countBeforePickingUp = getAllLoot().size();


            // TODO: If loot value is over X amount don't tele. Try to take it no matter what.
            item.interact("Take", LootingManager::hasPkerBeenDetected);

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

                if (Pricing.lookupPrice(item.getId()).orElse(0) >= 3800000) {
                    var outputFile = ScreenShotManager.takeScreenShotAndSave();

                    MyRevsClient.getScript().getLootWebhook().setUsername("Revenant Farm")
                            .setContent("**" + MyPlayer.getUsername() + " - Revs** - " +  "You have received a drop - **" + item.getName() + " - Value = " + Pricing.lookupPrice(item.getId()).orElse(0) + "**")
                            .addFile(outputFile)
                            .execute();
                }
                tripValue += Pricing.lookupPrice(item.getId()).orElse(0);
                totalValue += Pricing.lookupPrice(item.getId()).orElse(0);
                var totalString = MathUtility.getProfitPerHourString(totalValue);
                MyScriptVariables.setProfit(totalString);
            }
        }

        // starts back here with brea
        var allPossibleFood = getAllFood();

        for (int itemIndex = 0; itemIndex < allPossibleFood.size(); itemIndex++) {
            if (!Inventory.isFull() && Inventory.contains("Looting bag") && !getAllFood().isEmpty()) {
                var foodCount = Query.inventory().actionEquals("Eat").count();
                closeLootingBag();
                Log.debug("My inventory is not full, I have a looting bag, and there are angler or manta on the floor");
                var food = allPossibleFood.get(itemIndex);
                food.interact("Take", LootingManager::hasPkerBeenDetected);
                Log.debug("Count before pick: " + allPossibleFood.size());

                var pickedUp = Waiting.waitUntil(4000, () -> foodCount == Query.inventory().actionEquals("Eat").count() + 1);

                Log.debug("After picking up: " + getAllFood().size());
                if (hasPkerBeenDetected()) {
                    Log.debug("Pker has been detected. Cancelled further looting");
                    return;
                }

                if (!pickedUp) {
                    loot();
                }
            }
        }


        Log.debug("I'm done looting");

        if (RevkillerManager.getTarget() != null && RevkillerManager.getTarget().isValid()) {

            if (getTripValue() >= 200000) {
                new WorldTile(3205, 10082, 0).clickOnMinimap();
                Waiting.wait(2000);
                Equipment.Slot.RING.getItem().ifPresent(c -> c.click("Grand Exchange"));
                MyRevsClient.getScript().setState(State.BANKING);
                return;
            }else {
                if (!RevkillerManager.getTarget().isVisible()) {
                    RevkillerManager.getTarget().adjustCameraTo();
                }
                RevkillerManager.getTarget().click();
            }
        }

        if (Combat.isInWilderness() && MyRevsClient.myPlayerIsInCave()) {
            GlobalWalking.walkTo(MyRevsClient.getScript().getSelectedMonsterTile());
            MyRevsClient.getScript().setState(State.KILLING);
        }
        RevkillerManager.setHasClickedSpot(false);

        Log.debug("Ended looting process. Switching back to killing");
    }

    private static void closeLootingBag(){
        getLootingBag().filter(lootingBag -> lootingBag.getId() == 22586).ifPresent(lb -> {
            Log.debug("Closing looting bag");
            Waiting.waitUntil(() -> lb.click("Close"));
            Waiting.waitUntil(500, () -> Inventory.contains(11941));
        });

    }

    private static void openLootingBag() {
        getLootingBag().ifPresent(lootingBag -> {
            if (lootingBag.getId() == 11941) {
                lootingBag.click("Open");
                Waiting.waitUntil(500, () -> Inventory.contains(22586));
            }
        });
        if (Inventory.isFull() && !Inventory.contains("Looting bag") && Query.inventory().actionEquals("Eat").isAny()) {
            Query.inventory().actionEquals("Eat").findClosestToMouse().ifPresent(food -> food.click("Eat"));
        }
    }

    private static Optional<InventoryItem> getLootingBag() {
        return Query.inventory().nameEquals("Looting bag").findFirst();
    }

    private static List<GroundItem> getAllLoot() {
        return Query.groundItems()
                .nameEquals(lootToPickUp)
                .sorted(Comparator.comparingInt(item -> Pricing.lookupPrice(item.getId()).orElse(0)))
                .toList();
    }

    private static List<GroundItem> getAllFood(){
            return Query.groundItems()
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
