package scripts.rev;


import org.tribot.script.sdk.*;
import org.tribot.script.sdk.pricing.Pricing;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.GroundItem;
import org.tribot.script.sdk.types.InventoryItem;
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
            "Death rune", "Blood rune", "Blighted super restore(4)", "Onyx bolt tips", "Law rune", "Ring of wealth", "Blighted manta ray", "Blighted anglerfish"
    };
    private static int tripValue = 0;
    private static int totalValue = 0;

    public static void loot() {
        Log.debug("Started looting process");

        List<GroundItem> possibleLoot = getAllLoot();

        for (int itemIndex = 0; itemIndex < possibleLoot.size(); itemIndex++) {
            if (hasPkerBeenDetected() || Query.inventory().filter(food -> food.getActions().contains("Eat")).count() >= 15) {
                return;
            }

            Query.npcs().nameEquals("Revenant maledictus").findFirst().ifPresent(boss -> {
                if (boss.isValid() || boss.isAnimating() || boss.isMoving() || boss.isHealthBarVisible() || boss.getTile().isVisible() || boss.getTile().isRendered()){
                    TeleportManager.teleportOutOfWilderness("Boss has been seen! Trying to teleport out");
                    MyRevsClient.getScript().setState(State.BANKING);
                }
            });


            var item = possibleLoot.get(itemIndex);
            // Open the looting bag once you pick it up
            if (Query.inventory().filter(food -> food.getActions().contains("Eat")).count() < 14 && (item.getName().contains("anglerfish") || item.getName().contains("manta ray"))) {
                closeLootingBag();
            }else {
                openLootingBag();
            }


            if (!item.isVisible()) {
                item.adjustCameraTo();
            }

            var countBeforePickingUp = getAllLoot().size();


            // TODO: If loot value is over X amount don't tele. Try to take it no matter what.
            item.interact("Take", LootingManager::hasPkerBeenDetected);

            if (itemIndex == 0) {
                Log.debug("First item to pick up. Hovering over teleport in case pker is waiting.");
                Equipment.Slot.RING.getItem().ifPresent(ring -> {
                    ring.hoverMenu("Grand Exchange");

                });
            }

            if (hasPkerBeenDetected()) {
                Log.debug("Pker has been detected. Cancelled further looting");
                return;
            }

            var changed = Waiting.waitUntil(4000, () -> hasDecreased(countBeforePickingUp));

            if (!changed) {
                // FEELS LIKE THIS ONE PUTS IT BACK. ALWAYS FALSE?
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
        Log.debug("I'm done looting");

        if (RevkillerManager.getTarget() != null && RevkillerManager.getTarget().isValid()) {

            if (getTripValue() >= 200000) {
                Waiting.wait(1000);
                Equipment.Slot.RING.getItem().ifPresent(c -> c.click("Grand Exchange"));
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
        getLootingBag().ifPresent(lootingBag -> {
            if (lootingBag.getId() == 22586) {
                lootingBag.click("Close");
            }
        });
    }

    private static void openLootingBag() {
        getLootingBag().ifPresent(lootingBag -> {
            if (lootingBag.getId() == 11941) {
                lootingBag.click("Open");
            }
        });
        if (Inventory.isFull() && Inventory.contains("Shark")) {
            Query.inventory().nameEquals("Shark").findClosestToMouse().ifPresent(shark -> shark.click("Eat"));
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

    public static boolean hasPkerBeenDetected() {
        if (MyRevsClient.getScript().getPlayerDetectionThread() != null){
            return MyRevsClient.getScript().getPlayerDetectionThread().hasPkerBeenDetected();
        }
       return false;
    }

    public static boolean hasDecreased(int count) {
        Log.debug("Size of all loot: " + getAllLoot().size());
        Log.debug("Size of count: " + count);
        return getAllLoot().size() == count - 1 || getAllLoot().size() == 0;
    }

    public static boolean hasLootBeenDetected() {
        if (hasPkerBeenDetected()) {
            return false;
        }
        if (Query.inventory().filter(inventoryItem -> inventoryItem.getActions().contains("Eat")).count() >= 15) {
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
