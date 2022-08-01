package scripts.rev;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.interfaces.Identifiable;
import org.tribot.script.sdk.interfaces.Item;
import org.tribot.script.sdk.interfaces.Stackable;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.tasks.Amount;
import org.tribot.script.sdk.tasks.BankTask;
import org.tribot.script.sdk.tasks.EquipmentReq;
import org.tribot.script.sdk.tasks.ItemReq;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import scripts.api.Banker;
import scripts.api.Teleporting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static scripts.api.Banker.*;
import static scripts.api.Client.clickWidget;
import static scripts.api.Client.isWidgetVisible;


public class BankManagerRevenant {
    public static List<String> itemsToBuy = new ArrayList<>();

    private static BankTask equipmentBankTask = null;
    private static BankTask inventoryBankTask = null;


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
        openBank();
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
                    if (isWidgetVisible(584, 0)) {
                        clickWidget("Yes", 584, 1);
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

    public static void emptyLootingBag(){
        var lb = Query.inventory().nameEquals("Looting bag").findFirst().orElse(null);
        if (lb != null) {
            if (lb.click("View")) {
                if (isWidgetVisible(15, 3)) {
                    Waiting.waitUntil(() -> clickWidget("Deposit loot", 15, 8));
                    Waiting.waitUntil(() -> clickWidget("Dismiss", 15, 10));
                    Bank.close();
                }
            }
        }
    }


    public static void withdrawFoodAndPots() {
        // 1. Check if you have the items we need
        // 2. Withdraw items to inventory: Prayer pot, divine ranging pot, shark, stam, ring of dueling
        // 3. restock
        // 4. Pull out
        openBank();
        setPlaceHolder();

        if (!isInventoryBankTaskSatisfied()){
            Bank.depositInventory();
            checkIfNeedToRestockSupplies();
            getInventoryBankTask().execute();
        }


        // Take out our stuff

        openBank();
        emptyLootingBag();
        closeBank();

        if (Equipment.getAll().size() != 9) {
            Log.debug("I dont have 9 items on. Something went wrong... Trying again");
            withdrawGear();
        }

        if (!Teleporting.Dueling.feroxEnclave()){
            Log.debug("Couldn't teleport to ferox..");
        }

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



    public static BankTask getEquipmentBankTask() {
        if (equipmentBankTask == null) {
            Log.debug("equipment task");
            openBank();
            setPlaceHolder();
            Waiting.waitUntil(5000, Bank::isOpen);
            equipmentBankTask = BankTask.builder()
                    .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.RING).chargedItem("Ring of wealth", 1))
                    .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.HEAD).item(1169, Amount.of(1)))
                    .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.BODY).item(1129, Amount.of(1)))
                    .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.LEGS).item(2497, Amount.of(1)))
                    .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.FEET).item(1061, Amount.of(1)))
                    .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.CAPE).item(12273, Amount.of(1)))
                    .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.HANDS).item(21816, Amount.of(1)))
                    .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.WEAPON).item(22550, Amount.of(1)))
                    .addEquipmentItem(BankManagerRevenant::getAmulet)
                    .build();
        }

        return equipmentBankTask;
    }

    public static boolean useEtherOn(int itemId){
        return Query.inventory()
                .idEquals(itemId)
                .findFirst()
                .map(item -> Query.inventory()
                        .nameEquals("Revenant ether")
                        .findFirst()
                        .map(ether -> ether.useOn(item))
                        .orElse(false)).orElse(false);
    }

    public static boolean isEquipmentBankTaskSatisfied() {
        // check bracelet charges and bow charges are enough, if not recharge or return false

        return getEquipmentBankTask().isSatisfied();
    }

    public static void equipBowAndBraceAndChargeIfNeeded(){
        if (!equipBow()) {
            Log.warn("Failed to equip bow... if we see this error we should implement something");
        }
        if (!equipBracelet()){// lets separate the withdraw and equip and add the charge in the middle? sure
            // equip uncharged if no charged in the equip? this way the check will always find it
            // We need to charge an uncharged bracelet and equip it. I guess we are only looking for charged ones rn
            Log.warn("Failed to equip bracelet... could have none in bank. maybe uncharged?");
        }
        Waiting.waitUntil(() -> Equipment.contains(22550) && Equipment.contains(21816));
        EquipmentManager.checkCharges();
    }

    private static boolean withdrawBow() { // 22550 = charged bow
        if (Inventory.contains(22550) || Inventory.contains(22547)) return true;
        if (!Equipment.contains(22550) && !Equipment.contains(22547)){
            // this wait is unneccesary since it waits for the bank to be open in the openBank, so we can just call it
            //Waiting.waitUntil(BankManagerRevenant::openBank);
            openBank();
            if (Bank.contains(22550)) Bank.withdraw(22550, 1);
            else if (Bank.contains(22547)) Bank.withdraw(22547, 1);
            else {
                Log.warn("No bow... implement buying a bow?");
            }
            Waiting.waitUntil(2000, () -> Inventory.contains(22550) || Inventory.contains(22547));
        }
        return Inventory.contains(22550) || Inventory.contains(22547);
    }

    public static boolean hasEnoughEtherInBank(){
        return Query.bank().nameEquals("Revenant ether").findFirst().map(Stackable::getStack).orElse(0) < 1750 + 250;
    }

    private static boolean withdrawEther(int amount) {
        // Go buy if we dont have enoug ether
        if (!hasEnoughEtherInBank()){
            GrandExchangeRevManager.sellLoot();
            GrandExchangeRevManager.buyFromBank(21820, 4000);
        }
        // Withdraw it
        Bank.withdraw("Revenant ether", amount);
        return Waiting.waitUntil(2000, () -> Inventory.contains("Revenant ether"));

    }

    private static boolean inventoryContainsEther(int amount) {
        return Inventory.getCount("Revenant ether") >= amount;
    }

    private static boolean equipBow() {
        if (Equipment.contains(22550)) return true;
        if (withdrawBow()) {
            if (Inventory.contains(22550)){
                Query.inventory().idEquals(22550).findFirst().ifPresent(InventoryItem::click);
            }else {
                // Bow is uncharged
                if (!inventoryContainsEther(1750)) {
                    withdrawEther(1750);
                }
                if (useEtherOn(22547)){
                    Query.inventory().idEquals(22550).findFirst().ifPresent(InventoryItem::click);
                }else {
                    // Something went wrong. Couldn't use ether on
                    Log.debug("Something went wrong.. Couldn't use ether on bow");
                }

            }

            return Waiting.waitUntil(2000, () -> Equipment.contains(22550));
            //  you can shorten the wait because it should be done within a second or so, so if it's not, there's no reason to wait the full 11 seconds or so that it waits by default
        }
        return false;
    }

    private static boolean withdrawBracelet() {
        if (Inventory.contains("Bracelet of ethereum") || Inventory.contains("Bracelet of ethereum (uncharged)"))return true;
        if (!Equipment.contains("Bracelet of ethereum") && !Equipment.contains("Bracelet of ethereum (uncharged)")){
            Waiting.waitUntil(Banker::openBank);

            if (Bank.contains(21816)) {
                Bank.withdraw(21816, 1);
            }
            else if (Bank.contains(21817)){
                Bank.withdraw(21817, 1);
            }else {
                // No bracelet
            }
            Waiting.waitUntil(2000, () -> Inventory.contains("Bracelet of ethereum") || Inventory.contains("Bracelet of ethereum (uncharged)"));
            Waiting.waitUntil(Banker::closeBank);
            return Query.inventory().nameEquals("Bracelet of ethereum").findFirst().map(bow -> bow.click("Check")).orElse(false);
        }
        return Inventory.contains("Bracelet of ethereum") || Inventory.contains("Bracelet of ethereum (uncharged)");
    }

    private static boolean equipBracelet() {
        if (Equipment.contains(21816)) return true;
        if (withdrawBracelet()){
            if (Inventory.contains(21816)){
                Query.inventory().idEquals(21816).findFirst().ifPresent(InventoryItem::click);
            }else {
                if (!inventoryContainsEther(250)){
                    withdrawEther(250);
                }
                if (useEtherOn(21817)){
                    Query.inventory().idEquals(21816).findFirst().ifPresent(InventoryItem::click);
                }else {
                    // Something went wrong. Couldn't use ether on
                    Log.debug("Something went wrong.. Couldn't use ether on bracelet");
                }
            }
         }
        return false;
    }

    public static void withdrawGear() {
        Log.debug("Withdrawing gear");
        //1.
        //2.
        // get bracelet.. check charges
        // get bow
         // >.<
        Waiting.waitUntil(2000, Banker::openBank);
        equipBowAndBraceAndChargeIfNeeded(); // This method takes on bracelet and bows + checks their charges
        chargeBraceletOrBowIfNeeded();
        if (!isEquipmentBankTaskSatisfied()){
            checkIfNeedToBuyGear();
            getEquipmentBankTask().execute();
        }

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
            GrandExchangeRevManager.restockFromBank(itemsToBuy);
            Bank.depositInventory();

        }

}



    public static void init() {
        withdrawGear();
    }

}
