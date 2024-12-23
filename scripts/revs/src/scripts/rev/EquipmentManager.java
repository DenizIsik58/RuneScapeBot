package scripts.rev;

import org.tribot.script.sdk.Bank;
import org.tribot.script.sdk.Equipment;
import org.tribot.script.sdk.Inventory;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.EquipmentItem;
import scripts.api.MyBanker;
import scripts.api.MyExchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EquipmentManager {
    private static final List<String> basicGear = new ArrayList<>(Arrays.asList("Coif", "Bandos cloak", "Leather body", "Black d'hide chaps", "Leather boots", "Amulet of glory(6)", "Craw's bow", "Salve amulet(i)", "Salve amulet(ei)")); //

    private static final List<String> defenceGear = new ArrayList<>(Arrays.asList("Snakeskin bandana", "Bandos cloak", "Black d'hide body", "Black d'hide chaps", "Snakeskin boots", "Amulet of glory(6)", "Craw's bow", "Salve amulet(i)", "Salve amulet(ei)")); //

    private static final List<String> skulledGear = new ArrayList<>(Arrays.asList("Snakeskin bandana", "Rune arrow", "Black d'hide body", "Black d'hide chaps", "Snakeskin boots", "Magic shortbow", "Amulet of avarice")); //
    private static final List<String> pureGear = new ArrayList<>(Arrays.asList("Coif", "Rune arrow", "Leather body", "Black d'hide chaps", "Leather boots", "Magic shortbow", "Amulet of avarice" )); //

    private static int braceCharges = 0;
    private static int bowCharges = 0;

    public static boolean chargeAmmo(int arrowId, int arrowAmount) {
        var ammo = getAmmo(arrowId);
        if (ammo < arrowAmount) {
            MyBanker.openBank();
            if (Bank.getCount(arrowId) < 400) {
                GrandExchangeRevManager.restockFromBank(new ArrayList<>(Arrays.asList("Rune arrow")));
                MyBanker.depositAll();
            }
            MyBanker.withdraw(arrowId, arrowAmount - ammo, false);
            MyBanker.closeBank();
            Query.inventory().idEquals(arrowId).findFirst().map(c -> c.click("Wield"));
        }
        return Equipment.contains(arrowId);
    }

    private static int getAmmo(int arrowId) {
        return Equipment.getCount(arrowId);
    }

    public static int checkBraceletCharges() {
        MyBanker.closeBank();
        MyExchange.closeExchange();

        int currentBraceletCharges = braceCharges;

        if (Equipment.contains(21817)) {
            braceCharges = 0;
            return braceCharges;
        }
        if (Equipment.contains(21816)) {
            Query.equipment().idEquals(21816).findFirst().map(c -> c.click("Check"));
            Waiting.wait(2000);
        }

        if (Inventory.contains(21816)) {
            Query.inventory().idEquals(21816).findFirst().map(c -> c.click("Check"));
            Waiting.wait(2000);
        } else if (Inventory.contains(21817)) {
            braceCharges = 0;
            return braceCharges;
        }

        Waiting.waitUntil(1000, () -> braceCharges != currentBraceletCharges);
        return braceCharges;
    }

    public static int checkBowCharges() {
        MyBanker.closeBank();
        MyExchange.closeExchange();
        int currentBowCharge = bowCharges;

        if (Equipment.contains(22547)) {
            bowCharges = 0;
            return bowCharges;
        }
        if (Equipment.contains(22550)) {
            Query.equipment().idEquals(22550).findFirst().map(c -> c.click("Check"));
            Waiting.wait(2000);
        }

        if (Inventory.contains(22550)) {
            Query.inventory().idEquals(22550).findFirst().map(c -> c.click("Check"));
            Waiting.wait(2000);
        } else if (Inventory.contains(22547)) {
            bowCharges = 0;
            return bowCharges;
            // cutting out the wait if we know its 0
        }


        // it shouldnt take more than a second, so if it isnt changed in one second we will assume its the same as before

        Waiting.waitUntil(1000, () -> bowCharges != currentBowCharge);
        return bowCharges;
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

    public static boolean hasWealthCharges() {
        return Query.equipment().nameContains("Ring of wealth (").isAny();
    }

    public static void toggleBraceletAbsorbOn() {
        Equipment.Slot.HANDS.getItem().map(c -> c.click("Toggle absorption"));
    }

    public static List<String> getBasicGear() {
        return basicGear;
    }

    public static List<String> getDefenceGear() {
        return defenceGear;
    }

    public static List<String> getPureGear() {
        return pureGear;
    }

    public static List<String> getSkulledGear() {
        return skulledGear;
    }
}
