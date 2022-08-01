package scripts.rev;

import org.tribot.script.sdk.Equipment;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.InventoryItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EquipmentManager {
    private static final List<String> basicGear = new ArrayList<>(Arrays.asList("Coif", "Bandos cloak", "Leather body", "Black d'hide chaps", "Leather boots", "Amulet of glory(6)", "Craw's bow", "Salve amulet(i)", "Salve amulet(ei)")); //
    private static int braceCharges = 0;
    private static int bowCharges = 0;

    public static void equipGear(){
        for (var item : basicGear) {
            var gear = Query.inventory().nameEquals(item).findFirst().orElse(null);
            if (gear != null) {
                gear.click();
            }
        }
        Query.inventory().nameContains("Ring of wealth (").findFirst().map(InventoryItem::click);
        Query.inventory().nameContains("Bracelet").findFirst().map(InventoryItem::click);
    }

    public static void checkCharges(){
        Query.equipment().nameContains("Bracelet").findFirst().map(c -> c.click("Check"));
        Query.equipment().nameContains("Craw").findFirst().map(c -> c.click("Check"));
        Waiting.wait(3000);
    }

    public static int getBowCharges() {
        return bowCharges;
    }

    public static int getBraceCharges() {
        return braceCharges;
    }

    public static void setBowCharges(int bowCharges) {
        EquipmentManager.bowCharges = bowCharges;
    }

    public static void setBraceCharges(int braceCharges) {
        EquipmentManager.braceCharges = braceCharges;
    }

    public static boolean equipmentContains(String itemName){
        return Query.equipment().nameContains(itemName).isAny();
    }

    public static boolean hasWealthCharges(){
        return Query.equipment().nameContains("Ring of wealth (").isAny();
    }

    public static void toggleBraceletAbsorbOn(){
        Equipment.Slot.HANDS.getItem().map(c -> c.click("Toggle absorption"));
    }

    public static List<String> getBasicGear() {
        return basicGear;
    }
}
