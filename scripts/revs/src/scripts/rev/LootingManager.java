package scripts.rev;


import org.tribot.script.sdk.*;
import org.tribot.script.sdk.pricing.Pricing;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.walking.GlobalWalking;

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

        while(hasLootBeenDetected()){
            for (var loot : lootToPickUp){
                var item = Query.groundItems().nameEquals(loot).findFirst().orElse(null);
                if (item != null){
                    if (Inventory.isFull() && Inventory.contains("Shark")){
                        Query.inventory().nameEquals("Shark").findClosestToMouse().map(InventoryItem::click);
                    }

                    if (!item.isVisible()){
                        item.adjustCameraTo();
                    }

                    //Log.info(item);
                    var countBeforePickingUp = Query.groundItems().nameEquals(item.getName()).count();
                    item.hover();
                    item.click("Take");

                    Waiting.waitUntil(8000, () -> hasDecreased(item.getName(), countBeforePickingUp));

                    tripValue += Pricing.lookupPrice(item.getId()).orElse(0);
                    totalValue += Pricing.lookupPrice(item.getId()).orElse(0);
                    if (tripValue > 450000){
                        Log.debug("Teleporting out. I have: " + tripValue);
                        Equipment.Slot.RING.getItem().map(c -> c.click("Grand Exchange"));
                        Waiting.waitUntil(5000,MyRevsClient::myPlayerIsInGE);
                    }
                    break;
                }
        }
        }

            GlobalWalking.walkTo(RevenantScript.selectedMonsterTile);
            if(RevkillerManager.getTarget() != null && RevkillerManager.getTarget().isValid()){

               if (!RevkillerManager.getTarget().isVisible()){
                   RevkillerManager.getTarget().adjustCameraTo();
               }
                RevkillerManager.getTarget().click();

            }else {
                GlobalWalking.walkTo(RevenantScript.selectedMonsterTile);
            }

        RevenantScript.setState(State.KILLING);
    }

    public static boolean hasDecreased(String itemName, int count){
        return Query.groundItems().nameEquals(itemName).count() == count -1;
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
