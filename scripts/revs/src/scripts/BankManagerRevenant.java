package scripts;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.interfaces.Identifiable;
import org.tribot.script.sdk.interfaces.Item;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.tasks.Amount;
import org.tribot.script.sdk.tasks.BankTask;
import org.tribot.script.sdk.tasks.EquipmentReq;
import org.tribot.script.sdk.tasks.ItemReq;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BankManagerRevenant {
    public static List<String> itemsToBuy = new ArrayList<>();

    public static boolean openBank() {
        if (Bank.isOpen()) return true;
        if (!Bank.isNearby()) {
            GlobalWalking.walkToBank();
        }
        if (!Bank.isOpen()) {
            Bank.open();
        }
        Waiting.waitUntil(4000, Bank::isOpen);
        return Bank.isOpen();
    }

    public static boolean closeBank() {
        if (!Bank.isOpen()) return true;
        Bank.close();
        Waiting.waitUntil(4000, () -> !Bank.isOpen());
        return !Bank.isOpen();
    }

    public static void bankLoot() {
        if (!MyRevsClient.myPlayerIsInGE()) {
            Query.equipment().nameContains("Ring of wealth (").findFirst().map(ring -> ring.click("Grand exchange"));
            Waiting.waitUntil(MyRevsClient::myPlayerIsInGE);
        }
        returnFromTrip();
        //withdrawPVMInventory();
    }

    public static void checkIfWeHaveEmblemDrop() {
        List<String> valueAbles = new ArrayList<>(Arrays.asList("Ancient relic", "Ancient effigy", "Ancient medallion"));
        for (var item : valueAbles) {
            if (Query.bank().nameEquals(item).isAny()) {
                Log.debug("We have: " + item);
                GrandExchangeRevManager.sellLoot();
                return;
            }
        }
    }

    public static void returnFromTrip() {
        EquipmentManager.checkCharges();
        BankManagerRevenant.openBank();
        Waiting.waitUntil(Bank::isOpen);
        equipNewWealthIfNeeded();
        chargeBraceletOrBowIfNeeded();
        checkIfWeHaveEmblemDrop();
        withdrawFoodAndPots();
        WorldManager.hopToRandomMemberWorldWithRequirements();
        Waiting.wait(6000);
    }

    public static void equipNewWealthIfNeeded() {
        if (!EquipmentManager.hasWealthCharges()) {
            var ring = Query.bank().nameContains("Ring of wealth (").findFirst().orElse(null);
            if (ring != null) {
                BankTask.builder()
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.RING).chargedItem("Ring of wealth", 1)).build().execute();
            } else {
                Log.debug("Out of ring of wealths. Selling loot to buy more!");
                GrandExchangeRevManager.sellLoot();
                GrandExchangeRevManager.restockFromBank(new ArrayList<>(Arrays.asList("Ring of wealth (5)")));
            }
        }
    }

    public static void chargeBraceletOrBowIfNeeded() {
        // Check if we have enough ether in the bank
        if (!MyRevsClient.myPlayerHasEnoughChargesInBow()) {
            Log.debug(EquipmentManager.getBowCharges());
            Log.debug("bow needs ether. Charging....");
            openBank();
            if (Bank.getCount(21820) >= 500 + 100) {
                Bank.withdraw(21820, 500);
                Bank.close();
                Equipment.remove("Craw's bow");
                Waiting.waitUntil(() -> Inventory.contains(21820));
                Waiting.waitUntil(() -> Inventory.contains("Craw's bow"));
                Query.inventory()
                        .nameEquals("Craw's bow")
                        .findFirst()
                        .map(bow -> Query.inventory()
                                .idEquals(21820)
                                .findFirst()
                                .map(ether -> ether.useOn(bow))
                                .orElse(false));
                Waiting.wait(1000);
                Query.inventory().nameEquals("Craw's bow").findFirst().map(InventoryItem::click);
            } else {
                Bank.withdrawAll("Coins");

                GrandExchangeRevManager.buyFromBank(21820, 4000);
                chargeBraceletOrBowIfNeeded();
            }
        }
        if (MyRevsClient.myPlayerHasTooManyChargesInBrace()) {
            Log.debug("Bracelet has too much ether. Unloading...");
            if (openBank()) {
                closeBank();
            }
            var brace = Query.equipment().nameEquals("Bracelet of ethereum").findFirst().orElse(null);
            if (brace != null) {
                Waiting.waitUntil(1000, () -> Equipment.remove(brace.getId()) != 0);
                Waiting.waitUntil(() -> Inventory.contains("Bracelet of ethereum"));
                var invyBrace = Query.inventory().nameEquals("Bracelet of ethereum").findFirst().orElse(null);
                if (invyBrace != null) {
                    Waiting.waitUntil(() -> invyBrace.click("Uncharge"));
                    Waiting.waitUntil(ChatScreen::isOpen);
                    if (MyRevsClient.isWidgetVisible(584, 0)) {
                        MyRevsClient.clickWidget("Yes", 584, 1);
                        Waiting.waitUntil(() -> Inventory.contains("Revenant ether"));
                    }

                }
                openBank();
                var amount = Query.inventory().nameContains("Revenant eth").findFirst().map(InventoryItem::getStack).orElse(0);
                Waiting.waitUntil(() -> Bank.deposit("Revenant ether", amount - 250));
                Waiting.waitUntil(Bank::close);
                Waiting.wait(1000);
                Query.inventory()
                        .nameEquals("Bracelet of ethereum (uncharged)")
                        .findFirst()
                        .map(b -> Query.inventory()
                                .nameEquals("Revenant ether")
                                .findFirst()
                                .map(ether -> ether.useOn(b))
                                .orElse(false));

                Waiting.wait(1000);
                Query.inventory().nameEquals("Bracelet of ethereum").findFirst().map(InventoryItem::click);

            }

        }

        if (!MyRevsClient.myPlayerHasEnoughChargesInBracelet()) {
            openBank();
            Bank.withdraw("Revenant ether", 100);
            Bank.close();
            Equipment.remove("Bracelet of ethereum");
            Waiting.waitUntil(() -> Inventory.contains("Revenant ether"));
            Waiting.waitUntil(() -> Inventory.contains("Bracelet of ethereum"));
            Query.inventory()
                    .nameEquals("Bracelet of ethereum")
                    .findFirst()
                    .map(brace -> Query.inventory()
                            .nameEquals("Revenant ether")
                            .findFirst()
                            .map(ether -> ether.useOn(brace))
                            .orElse(false));
            Query.inventory().nameEquals("Bracelet of ethereum").findFirst().map(InventoryItem::click);
        }

    }

    public static void checkIfNeedToRestockSupplies(){
        List<String> itemsToBuy = new ArrayList<>();

        if (!Query.bank().nameContains("divine ranging").isAny()) {

            itemsToBuy.add("Divine ranging potion(4)");
        }

        if (!Query.bank().nameContains("Stamina potion").isAny()) {
            itemsToBuy.add("Stamina potion(4)");
        }

        if (!Query.bank().nameContains("Ring of dueling(").isAny()) {
            itemsToBuy.add("Ring of dueling(8)");
        }

        if (Bank.getCount("Shark") < 15) {
            itemsToBuy.add("Shark");
        }

        if (Bank.getCount("Prayer potion(4)") < 5) {
            itemsToBuy.add("Prayer potion(4)");
        }

        // Buy items if we need
        if (itemsToBuy.size() != 0) {
            itemsToBuy.forEach(Log::info);
            if (!GrandExchange.isNearby()) {
                GlobalWalking.walkTo(new WorldTile(3164, 3484, 0));
            }
            Log.debug("I'm out of supplies. Selling loot and buying more.");
            GrandExchangeRevManager.sellLoot();
            openBank();
            GrandExchangeRevManager.restockFromBank(itemsToBuy);
            Bank.depositInventory();
            withdrawGear();
        }
    }


    public static void withdrawFoodAndPots() {
        // 1. Check if you have the items we need
        // 2. Withdraw items to inventory: Prayer pot, divine ranging pot, shark, stam, ring of dueling
        // 3. restock
        // 4. Pull out
        openBank();
        setPlaceHolder();
        Bank.depositInventory();

        if (!isInventoryBankTaskSatisfied()){
            checkIfNeedToRestockSupplies();
            getInventoryBankTask();
        }

        // Take out our stuff

        openBank();
        var lb = Query.inventory().nameEquals("Looting bag").findFirst().orElse(null);
        if (lb != null) {
            if (lb.click("View")) {
                if (MyRevsClient.isWidgetVisible(15, 3)) {
                    Waiting.waitUntil(() -> MyRevsClient.clickWidget("Deposit loot", 15, 8));
                    Waiting.waitUntil(() -> MyRevsClient.clickWidget("Dismiss", 15, 10));
                    Bank.close();
                }
            }
        }
        closeBank();

        if (Equipment.getAll().size() != 9) {
            Log.debug("I dont have 9 items on");
            withdrawGear();
        }

        Log.debug("End of withdrawpotsandfood");

        Query.inventory().nameContains("Ring of dueling (").findFirst().map(c -> c.click("Rub"));
        Waiting.waitUntil(2000, () -> ChatScreen.containsOption("Ferox Enclave."));
        ChatScreen.selectOption("Ferox enclave.");

    }

    public static void goToGeIfNotThere() {
        if (!GrandExchange.isNearby()) {
            openBank();
            if (!Query.inventory().nameContains("Ring of wealth (").isAny() || !Query.equipment().nameContains("Ring of wealth (").isAny()) {
                openBank();
                BankTask.builder().addEquipmentItem(EquipmentReq.slot(Equipment.Slot.RING).chargedItem("Ring of wealth", 1)).build().execute();
                Bank.close();
            }
            Equipment.Slot.RING.getItem().map(c -> c.click("Grand exchange"));
            Waiting.waitUntil(MyRevsClient::myPlayerIsInGE);
        }
    }

    private static Optional<Item> getBankGlory() {
        return Query.bank().nameContains("Amulet of glory(").findFirst();
    }

    private static boolean bankContainsGlory() {
        return getBankGlory().isPresent();
    }

    private static EquipmentReq getAmulet() {
        int id = 0;

        if (Bank.contains("Salve amulet(ei)") || Equipment.contains("Salve amulet(ei)") || Inventory.contains("Salve amulet(ei)")){
            id = 25278;
        } else if (Bank.contains("Salve amulet(i)") || Equipment.contains("Salve amulet(i)") || Inventory.contains("Salve amulet(i)")) {
            id = 12017;
        }

        if (id != 0){
            return EquipmentReq.slot(Equipment.Slot.NECK).item(id, Amount.of(1));
        } else {
            return EquipmentReq.slot(Equipment.Slot.NECK).chargedItem("Amulet of glory", 1);
        }
    }

    private static EquipmentReq getBow() {
        EquipmentReq craw = null;
        if (Bank.contains(22550) || Inventory.contains(22550) || Equipment.contains(22550)) {
            Log.debug("I have a craw");
            craw = EquipmentReq.slot(Equipment.Slot.WEAPON).item(22550, Amount.of(1));
        } else if (Bank.contains(22547)) {
            Log.debug("I have a craw u");
            Bank.withdraw(22547, 1);
            if (Bank.getCount(21820) < 1750 + 250) {
                Log.debug("I'm out of ether. Selling loot and buying more");
                GrandExchangeRevManager.sellLoot();
                GrandExchangeRevManager.buyFromBank(21820, 4000);
            }

            Waiting.waitUntil(() -> Bank.withdraw(21820, 1750));
            Waiting.waitUntil(() -> Bank.withdraw("Craw's bow (u)", 1));
            Bank.close();
            Waiting.waitUntil(2000, () -> Query.inventory().idEquals(21820).isAny());
            Query.inventory()
                    .nameEquals("Craw's bow (u)")
                    .findFirst()
                    .map(bow -> Query.inventory()
                            .nameEquals("Revenant ether")
                            .findFirst()
                            .map(ether -> ether.useOn(bow))
                            .orElse(false));
            openBank();
            Waiting.waitUntil(Bank::isOpen);
            craw = EquipmentReq.slot(Equipment.Slot.WEAPON).item(22550, Amount.of(1));
        }
        return craw;
    }

    private static EquipmentReq getBracelet() {
        EquipmentReq bracelet = null;

        if (Bank.contains("Bracelet of ethereum")) {
            Log.debug("I have a bracelet");
            bracelet = EquipmentReq.slot(Equipment.Slot.HANDS).item(21816, Amount.of(1));
        } else if (Bank.contains("Bracelet of ethereum (uncharged)")) {
            Log.debug("I have a brace uncharged");
            Waiting.waitUntil(() -> Bank.withdraw("Bracelet of ethereum (uncharged)", 1));

            if (Bank.getCount(21820) < 250) {
                Log.debug("I'm out of ether. Selling loot and buying more");
                GrandExchangeRevManager.sellLoot();
                GrandExchangeRevManager.buyFromBank(21820, 4000);

            }
            Waiting.waitUntil(() -> Bank.withdraw(21820, 250));
            Bank.close();
            Waiting.waitUntil(2000, () -> Query.inventory().idEquals(21820).isAny());
            Query.inventory()
                    .nameEquals("Bracelet of ethereum (uncharged)")
                    .findFirst()
                    .map(brace -> Query.inventory()
                            .nameEquals("Revenant ether")
                            .findFirst()
                            .map(ether -> ether.useOn(brace))
                            .orElse(false));
            bracelet = EquipmentReq.slot(Equipment.Slot.HANDS).item(21816, Amount.of(1));
        }
        return bracelet;
    }

    public static BankTask getEquipmentBankTask() {
        openBank();
        setPlaceHolder();
        Waiting.waitUntil(5000, Bank::isOpen);

        return BankTask.builder()
                .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.RING).chargedItem("Ring of wealth", 1))
                .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.HEAD).item(1169, Amount.of(1)))
                .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.BODY).item(1129, Amount.of(1)))
                .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.LEGS).item(2497, Amount.of(1)))
                .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.FEET).item(1061, Amount.of(1)))
                .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.CAPE).item(12273, Amount.of(1)))
                .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.HANDS).item(21816, Amount.of(1)))
                .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.WEAPON).item(22550, Amount.of(1)))
                .addEquipmentItem(BankManagerRevenant::getAmulet).addEquipmentItem(BankManagerRevenant::getBow)
                .addEquipmentItem(BankManagerRevenant::getBracelet)
                .build();
    }

    public static boolean isEquipmentBankTaskSatisfied() {

        // if need to recharge bracelet, recharge it here... if no ether return false

        return getEquipmentBankTask().isSatisfied();
    }

    public static void withdrawGear() {
        Waiting.waitUntil(BankManagerRevenant::openBank);
        Waiting.waitUntil(Bank::depositInventory);
        Waiting.waitUntil(Bank::depositEquipment);

        // if needs to charge bracelet or bow, do it here..

        //EquipmentManager.checkCharges(); // Check our bracelet
        //chargeBraceletOrBowIfNeeded(); // Charge if needed


        checkIfNeedToBuyGear();
        getEquipmentBankTask().execute();

        Log.debug(isEquipmentBankTaskSatisfied());

        EquipmentManager.checkCharges();
        //chargeBraceletOrBowIfNeeded();

        withdrawFoodAndPots();

        // if need to buy anything, can do it here or handle it here anyways


    }

    public static BankTask getInventoryBankTask() {
        return BankTask.builder()
                .addInvItem(2434, Amount.of(5)) // Prayer pot
                .addInvItem(385, Amount.of(15))
                .addInvItem(() -> {
                    var id = Query.bank().nameContains("Ring of dueling(").findFirst().map(Identifiable::getId).orElse(0);
                    var amount = id == 0 ? Amount.of(0) : Amount.of(1);
                    return new ItemReq(id, amount);
                })
                .addInvItem(() -> {
                    var id = Query.bank().nameContains("Stamina potion(").findFirst().map(Identifiable::getId).orElse(0);
                    var amount = id == 0 ? Amount.of(0) : Amount.of(1);
                    return new ItemReq(id, amount);
                })
                .addInvItem(() -> {
                    var id = Query.bank().nameContains("Divine ranging potion(").findFirst().map(Identifiable::getId).orElse(0);
                    var amount = id == 0 ? Amount.of(0) : Amount.of(1);
                    return new ItemReq(id, amount);
                })
                .addInvItem(() -> {
                    var id = Query.bank().nameEquals("Looting bag").findFirst().map(Identifiable::getId).orElse(0);
                    var amount = id == 0 ? Amount.of(0) : Amount.of(1);
                    return new ItemReq(id, amount);
                })// Shark
                .build();
    }

    public static boolean isInventoryBankTaskSatisfied() {

        return getInventoryBankTask().isSatisfied();
    }


    public static void checkIfNeedToBuyGear() {

        openBank();
        setPlaceHolder();

        List<String> itemsToBuy = new ArrayList<>();


        for (var item : EquipmentManager.getBasicGear()) {

            if (item.equals("Craw's bow") || item.equals("Salve amulet(i)") || item.equals("Salve amulet(ei)")) {
                continue;
            }
            if (!Query.bank().nameEquals(item).isAny()) {
                Log.debug("We don't have: " + item + ". Added to list");
                itemsToBuy.add(item);
            }

        }
        if (!Query.bank().nameContains("Ring of wealth (").isAny()) {
            Log.debug("We're out of wealths. Added to list.");
            itemsToBuy.add("Ring of wealth (5)");
        }

        if (!Query.bank().nameEquals("Bracelet of ethereum").isAny() && !Query.bank().nameEquals("Bracelet of ethereum (uncharged)").isAny()) {
            Log.debug("We're out of bracelets. Added to list.");
            itemsToBuy.add("Bracelet of ethereum (uncharged)");
        }

        if(itemsToBuy.size() !=0) {
            if (!GrandExchange.isNearby()) {
                GlobalWalking.walkTo(new WorldTile(3164, 3484, 0));
            }
            GrandExchangeRevManager.sellLoot();
            openBank();
            GrandExchangeRevManager.restockFromBank(itemsToBuy);
            Bank.depositInventory();

        }

}




    public static void setPlaceHolder(){
            if (!BankSettings.isPlaceholdersEnabled()){
                Waiting.waitUntil(() -> MyRevsClient.clickWidget("Enable", 12, 38));
            }
    }


    public static void init() {
        withdrawGear();
    }

}
