package scripts;


import org.tribot.script.sdk.Inventory;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.MyPlayer;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.pricing.Pricing;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.GroundItem;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.walking.GlobalWalking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

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
                    "Blighted super restore(4)", "Onyx bolt tips", "Law rune"));
    private static int tripValue = 0;
    private static int totalValue = 0;

    public static void loot(){

            for (var loot : lootToPickUp){
                var item = Query.groundItems().nameEquals(loot).findFirst().orElse(null);
                if (item != null){
                    if (Inventory.isFull() && Inventory.contains("Shark")){
                        Query.inventory().nameEquals("Shark").findClosestToMouse().map(InventoryItem::click);
                        Waiting.wait(1500);
                    }

                    if (!item.isVisible()){
                        item.adjustCameraTo();
                    }
                    //Log.info(item);
                    var countBeforePickingUp = Query.groundItems().nameEquals(item.getName()).count();
                    item.hover();
                    item.interact("Take");

                    while(true){
                        if (MyPlayer.getCurrentHealthPercent() <= 10 || Inventory.getCount("Shark") == 0 || Query.inventory().nameContains("Prayer pot").count() == 0){
                            PkerDetecter.quickTele();
                            Waiting.wait(100);
                        }

                        //Log.info(countBeforePickingUp);
                        if (hasDecreased(item.getName(), countBeforePickingUp)){
                            break;
                        }
                        Waiting.wait(20);
                    }

                    tripValue += Pricing.lookupPrice(item.getId()).orElse(0);
                    totalValue += tripValue;
                    Log.info(tripValue);

                    return;
                }
        }


        RevenantScript.state = State.KILLING;
        RevenantScript.selectedMonsterTile.click();

    }

    public static boolean hasDecreased(String itemName, int count){
        return Query.groundItems().nameEquals(itemName).count() == count -1;
    }
    public static void lot() {
        if (!hasLootBeenDetected()){
            return;
        }

        for (var item : lootToPickUp) {
            var loot = Query.groundItems().nameContains(item).findFirst().orElse(null);
            if (loot != null) {
                loot.interact("Take", () -> !loot.isVisible());
                //Log.info("Picking up: " + loot.getName());
                tripValue += Pricing.lookupPrice(loot.getId()).orElse(0);
            }
        }

    }

    public static boolean hasLootBeenDetected() {
        for (var item : lootToPickUp) {
            if (Query.groundItems().nameEquals(item).isAny()) {
                //Log.info("Found loot!");
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
