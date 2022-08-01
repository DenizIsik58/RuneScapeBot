package scripts.api;

import org.tribot.script.sdk.Bank;
import org.tribot.script.sdk.BankSettings;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.util.Retry;
import org.tribot.script.sdk.walking.GlobalWalking;

public class Banker {

    public static boolean openBank() {
        if (Bank.isOpen()) return true;
        if (!walkToBank()) {
            Log.warn("Failed to open the bank because walking to the bank failed.");
            return false;
        };
        if (!Bank.isOpen()) {
            Bank.open();
        }
        return Waiting.waitUntil(4000, Bank::isOpen);
    }

    public static boolean closeBank() {
        if (!Bank.isOpen()) return true;
        Bank.close();
        return Waiting.waitUntil(4000, () -> !Bank.isOpen());
    }

    // this is useful later when/if you run into an area that you want to go to a bank different than where dax tries to go
    // for example in the mining guild if you call GlobalWalking.walkToBank() it will not take you to the mining guild bank for some reason
    // so you can add something like "if in mining guild, go to mining guild bank"
    // for now it will look useless, but its worth implementing early
    public static boolean walkToBank() {
        if (Bank.isNearby()) return true;
        GlobalWalking.walkToBank();
        return Waiting.waitUntil(2000, Bank::isNearby);
    }

    private static boolean setNoted(boolean noted) {
        if (!Bank.isOpen()) return false;
        return Retry.retry(3, () -> {
            boolean isNotedEnabled = BankSettings.isNoteEnabled();
            if (isNotedEnabled == noted) return true;
            if (!noted) return BankSettings.setNoteEnabled(false);
            BankSettings.setNoteEnabled(true);
            return Waiting.waitUntil(500, BankSettings::isNoteEnabled);
        });
    }

    private static int getInventoryCount(String name, boolean noted) {
        return noted ? Query.inventory().nameEquals(name).isNoted().sumStacks()
                : Query.inventory().nameEquals(name).isNotNoted().sumStacks();
    }

    public static boolean withdraw(int id, int amount, boolean noted) {
        return withdraw(getName(id), amount, noted);
    }

    public static boolean withdraw(String name, int amount, boolean noted) {
        if (!openBank()) {
            Log.warn("Failed to withdraw because could not open bank.");
            return false;
        }
        setNoted(noted);
        var bankCount = Bank.getCount(name);
        var startingInventoryCount = getInventoryCount(name, noted);
        if (bankCount < amount) {
            Log.warn("We do not have " + name + " x" + amount + " in the bank, withdrawing all.");
            Bank.withdrawAll(name);
            Waiting.waitUntil(2000, () -> getInventoryCount(name, noted) == startingInventoryCount + bankCount);
            // always false because we didnt have how many we wanted
            return false;
        }
        Bank.withdraw(name, amount);
        return Waiting.waitUntil(2000, () -> getInventoryCount(name, noted) == startingInventoryCount + amount);
    }



    private static String getName(int id) {
        var definition = Query.itemDefinitions()
                .idEquals(id)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find item with ID: " + id));
        return definition.getName();
    }

    public static void setPlaceHolder(){
        if (!BankSettings.isPlaceholdersEnabled()){
            Waiting.waitUntil(() -> Client.clickWidget("Enable", 12, 38));
        }
    }


}
