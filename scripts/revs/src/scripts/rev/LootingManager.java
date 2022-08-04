package scripts.rev;


import org.tribot.script.sdk.Combat;
import org.tribot.script.sdk.Inventory;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.pricing.Pricing;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.walking.GlobalWalking;
import scripts.api.MyScriptVariables;
import scripts.api.utility.MathUtility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LootingManager {

    private static final List<String> lootToPickUp =
            new ArrayList<>(Arrays.asList("Bracelet of ethereum (uncharged)", "Battlestaff", "Rune full helm",
                    "Rune platebody", "Rune platelegs", "Rune kiteshield", "Rune warhammer", "Dragon dagger",
                    "Dragon longsword", "Dragon platelegs", "Dragon plateskirt",
                    "Dragon med helm", "Coal", "Adamantite bar", "Runite ore", "Black dragonhide",
                    "Yew logs", "Runite bar", "Mahogany plank", "Magic logs", "Uncut dragonstone", "Yew seed",
                    "Magic seed", "Amulet of avarice", "Craw's bow (u)", "Thammaron's sceptre (u)",
                    "Viggora's chainmace (u)", "Ancient emblem", "Ancient totem", "Ancient crystal",
                    "Ancient statuette", "Ancient medallion", "Ancient effigy", "Ancient relic",
                    "Looting bag", "Dragonstone bolt tips", "Death rune", "Blood rune",
                    "Blighted super restore(4)", "Onyx bolt tips", "Law rune", "Ring of wealth"));
    private static int tripValue = 0;
    private static int totalValue = 0;

    public static void loot(){
        Log.debug("Started looting process");

        if (setStateBankIfNotInWilderness()){
            return;
        }

        while(hasLootBeenDetected()){
            for (var loot : lootToPickUp){
                Query.groundItems().nameEquals(loot).findFirst().ifPresent(item -> {
                    if (setStateBankIfNotInWilderness()){
                        return;
                    }
                    if (Inventory.isFull() && Inventory.contains("Shark")){
                        Query.inventory().nameEquals("Shark").findClosestToMouse().map(InventoryItem::click);
                    }

                    if (!item.isVisible()){
                        item.adjustCameraTo();
                    }

                    //STOPS HERE
                    Log.debug("Picking up item: " + item.getName());
                    var countBeforePickingUp = Query.groundItems().nameEquals(item.getName()).count();

                    item.click("Take");

                    var changed = Waiting.waitUntil(4000, () -> hasDecreased(item.getName(), countBeforePickingUp));
                    if (!changed) return;

                    tripValue += Pricing.lookupPrice(item.getId()).orElse(0);
                    totalValue += Pricing.lookupPrice(item.getId()).orElse(0);
                    var totalString = MathUtility.getProfitPerHourString(totalValue);
                    MyScriptVariables.setProfit(totalString);
                    if (tripValue > 450000){
                        TeleportManager.teleportOutOfWilderness("Teleporting out. I have: " + tripValue + " gold!");

                        // teleport out
                    }
                });
        }
        }
        // starts back here with brea
            Log.debug("I'm done looting");
            GlobalWalking.walkTo(MyRevsClient.getScript().getSelectedMonsterTile());
            if(RevkillerManager.getTarget() != null && RevkillerManager.getTarget().isValid()){

               if (!RevkillerManager.getTarget().isVisible()){
                   RevkillerManager.getTarget().adjustCameraTo();
               }
                RevkillerManager.getTarget().click();

            }else {
                GlobalWalking.walkTo(MyRevsClient.getScript().getSelectedMonsterTile());
            }

            if (setStateBankIfNotInWilderness()){
                return;
            }
            if (Combat.isInWilderness() && MyRevsClient.myPlayerIsInCave()){
                MyRevsClient.getScript().setState(State.KILLING);
            }

            Log.debug("Ended looting process. Switching back to killing");
    }

    public static boolean hasDecreased(String itemName, int count){
        return Query.groundItems().nameEquals(itemName).count() == count -1;
    }

    public static boolean setStateBankIfNotInWilderness(){
        if (!Combat.isInWilderness() && MyRevsClient.myPlayerIsInCave()){
            MyRevsClient.getScript().setState(State.BANKING);
            return true;
        }
        return false;
    }

    public static boolean hasLootBeenDetected() {
        for (var item : lootToPickUp) {
            if (Query.groundItems().nameEquals(item).isAny()) {
                return true;
            }
        }
        return false;
    }

    public static int getTotalValue() {
        return totalValue;
    }

    public static void setTotalValue(int totalValue) {
        LootingManager.totalValue = totalValue;
    }

    public static List<String> getLootToPickUp() {
        return lootToPickUp;
    }

    public static int getTripValue() {
        return tripValue;
    }

    public static void resetTripValue() {
        LootingManager.tripValue = 0;
    }
}
