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
            "Death rune", "Blood rune", "Blighted super restore(4)", "Onyx bolt tips", "Law rune", "Ring of wealth"
    };
    private static int tripValue = 0;
    private static int totalValue = 0;

    public static void loot() {
        Log.debug("Started looting process");

        Optional<GroundItem> lootOptional;

        List<GroundItem> possibleLoot = getAllLoot();

        for (int itemIndex = 0; itemIndex < possibleLoot.size(); itemIndex++) {
            if (hasPkerBeenDetected()) return;

            var item = possibleLoot.get(itemIndex);
            // Open the looting bag once you pick it up

            openLootingBag();

            if (!item.isVisible()) {
                item.adjustCameraTo();
            }

            //STOPS HERE
            Log.debug("Picking up item: " + item.getName());
            var countBeforePickingUp = getAllLoot().size();
            Log.debug("Count before picking up: " + countBeforePickingUp);


            // TODO: If loot value is over X amount don't tele. Try to take it no matter what.
            item.interact("Take", () -> hasPkerBeenDetected() || LootingManager.tripValue < 450000);

            if (itemIndex == 0) {
                // HOVERS HERE AND DOESN'T FINISH THE LOOP
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
                tripValue += Pricing.lookupPrice(item.getId()).orElse(0);
                totalValue += Pricing.lookupPrice(item.getId()).orElse(0);
                var totalString = MathUtility.getProfitPerHourString(totalValue);
                MyScriptVariables.setProfit(totalString);
                if (tripValue > 450000) {
                    TeleportManager.teleportOutOfWilderness("Teleporting out. I have: " + tripValue + " gold!");

                    // teleport out
                }
            }
        }


        // starts back here with brea
        Log.debug("I'm done looting");

        if (RevkillerManager.getTarget() != null && RevkillerManager.getTarget().isValid()) {

            if (!RevkillerManager.getTarget().isVisible()) {
                RevkillerManager.getTarget().adjustCameraTo();
            }
            RevkillerManager.getTarget().click();

        }

        if (Combat.isInWilderness() && MyRevsClient.myPlayerIsInCave()) {
            GlobalWalking.walkTo(MyRevsClient.getScript().getSelectedMonsterTile());
            MyRevsClient.getScript().setState(State.KILLING);
        }

        Log.debug("Ended looting process. Switching back to killing");
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

    private static boolean hasPkerBeenDetected() {
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
        if (hasPkerBeenDetected()) return false;
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
