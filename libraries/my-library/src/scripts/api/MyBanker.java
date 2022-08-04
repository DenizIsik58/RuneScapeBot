package scripts.api;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.definitions.ItemDefinition;
import org.tribot.script.sdk.util.Retry;
import org.tribot.script.sdk.walking.GlobalWalking;

public class MyBanker {

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
        return withdraw(getItemDefinition(id).getName(), amount, noted);
    }

    public static boolean withdraw(String name, int amount, boolean noted) {
        if (!openBank()) {
            Log.warn("Failed to withdraw [" + name + "] because could not open bank.");
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

    public static boolean deposit(String itemName, int amount, boolean noted) {
        return deposit(getItemDefinition(itemName), amount, noted);
    }

    public static boolean deposit(int id, int amount, boolean noted) {
        return deposit(getItemDefinition(id), amount, noted);
    }

    public static boolean deposit(ItemDefinition definition, int amount, boolean noted) {
        if (!openBank()) {
            Log.warn("Failed to deposit [" + definition.getName() + "] because could not open bank.");
            return false;
        }
        var inventoryCount = getInventoryCount(definition.getName(), noted);
        int id = getCorrectId(definition, noted);
        if (amount >= inventoryCount) Bank.depositAll(id);
        else Bank.deposit(id, amount);
        return Waiting.waitUntil(2000, () -> getInventoryCount(definition.getName(), noted) == Math.max(0, inventoryCount - amount));
    }

    public static boolean depositAll(){

        if (!Inventory.isEmpty()){
            Bank.depositInventory();
            Waiting.waitUntil(3000, Inventory::isEmpty);
        }

        return true;
    }


    private static int getCorrectId(ItemDefinition definition, boolean noted) {
        if (noted) {
            return definition.getNotedItemId() == 0 ? definition.getUnnotedItemId() : definition.getNotedItemId();
        } else {
            return definition.getUnnotedItemId();
        }
    }

    private static ItemDefinition getItemDefinition(String name) {
        return Query.itemDefinitions()
                .nameEquals(name)
                .isNotPlaceholder()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No item found with name: " + name));
    }

    private static ItemDefinition getItemDefinition(int id) {
        return Query.itemDefinitions()
                .idEquals(id)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find item with ID: " + id));
    }

    public static void setPlaceHolder(){
        if (!BankSettings.isPlaceholdersEnabled()){
            Waiting.waitUntil(() -> MyClient.clickWidget("Enable", 12, 38));
        }
    }



}
