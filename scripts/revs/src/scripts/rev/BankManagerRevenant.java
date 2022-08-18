package scripts.rev;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.interfaces.Identifiable;
import org.tribot.script.sdk.interfaces.Stackable;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.tasks.Amount;
import org.tribot.script.sdk.tasks.BankTask;
import org.tribot.script.sdk.tasks.EquipmentReq;
import org.tribot.script.sdk.tasks.ItemReq;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import scripts.api.MyBanker;
import scripts.api.MyExchange;
import scripts.api.MyTeleporting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static scripts.api.MyBanker.*;
import static scripts.api.MyClient.clickWidget;
import static scripts.api.MyClient.isWidgetVisible;


public class BankManagerRevenant {

    private static AtomicInteger withdrawGearAttempts = new AtomicInteger(0);
    private static BankTask equipmentBankTask = null;
    private static boolean isUsingGlory = true;

    public static void init() {
        PrayerManager.turnOffAllPrayer();
        drinkAntiVenom();
        withdrawGear();
    }

    public static void bankLoot() {
        returnFromTrip();
        //withdrawPVMInventory();
    }

    public static void checkIfWeHaveEmblemDrop() {
        MyBanker.openBank();
        List<String> valueAbles = new ArrayList<>(Arrays.asList("Ancient relic", "Ancient effigy", "Ancient medallion"));
        for (var item : valueAbles) {
            if (Query.bank().nameEquals(item).isAny()) {
                Log.debug("We have: " + item);
                GrandExchangeRevManager.sellLoot();
                return;
            }
        }
    }

    private static void drinkAntiVenom(){
        if (MyPlayer.isPoisoned()) {
            MyBanker.openBank();
            Query.bank().nameContains("Anti-venom(").findFirst().map(anti -> MyBanker.withdraw(anti.getName(), 1, false));
            Query.inventory().nameContains("Anti-venom(").findClosestToMouse().map(anti -> anti.click("Drink"));
            var drink = Waiting.waitUntil(2000, () -> !MyPlayer.isPoisoned());
            if (!drink) {
                drinkAntiVenom();
            }
            Query.inventory().nameContains("Anti-venom(").findClosestToMouse().map(anti -> MyBanker.deposit(anti.getId(), 1, false));
        }
    }

    public static void returnFromTrip() {
        //EquipmentManager.checkCharges();
        Waiting.waitUntil(5000, MyRevsClient::myPlayerIsInGE);
        PrayerManager.turnOffAllPrayer();
        MyBanker.openBank();
        drinkAntiVenom();
        equipAndChargeItems();
        equipNewWealthIfNeeded();
        checkIfWeHaveEmblemDrop();
        withdrawFoodAndPots();
        WorldManager.hopToRandomMemberWorldWithRequirements();
        Waiting.wait(6000);
    }

    public static void equipAndChargeItems() {
        /*if (Skill.RANGED.getActualLevel() < 75) {
            equipAndCharge(true);
        }*/
        equipAndCharge(true);
        equipAndCharge(false);
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
                GrandExchangeRevManager.restockFromBank(List.of("Ring of wealth (5)"));
            }
        }
    }

    public static void checkIfNeedToRestockSupplies() {
        List<String> itemsToBuy = new ArrayList<>();

        if (!Query.bank().nameContains("divine ranging").isAny()) {

            itemsToBuy.add("Divine ranging potion(4)");
        }

        if (!Query.bank().nameContains("Anti-venom(").isAny()) {

            itemsToBuy.add("Anti-venom(4)");
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

        if (Bank.getCount("Blighted super restore(4)") < 4) {
            itemsToBuy.add("Blighted super restore(4)");
        }

        if (Bank.getCount("Saradomin brew(4)") < 3) {
            itemsToBuy.add("Saradomin brew(4)");
        }

        if (!Query.bank().nameContains("Ring of recoil").isAny()) {
            itemsToBuy.add("Ring of recoil");
        }



        // Buy items if we need
        if (itemsToBuy.size() != 0) {
            itemsToBuy.forEach(Log::info);
            MyExchange.walkToGrandExchange();
            DecantManager.decantPotionsFromBank();
            Log.debug("I'm out of supplies. Selling loot and buying more.");
            GrandExchangeRevManager.sellLoot();
            openBank();
            GrandExchangeRevManager.restockFromBank(itemsToBuy);
            Bank.depositInventory();
            withdrawGear();
        }
    }

    public static void emptyLootingBag() {
        if (!Inventory.contains("Looting bag")){
            if (!Bank.contains("Looting bag")){
                Log.debug("We have no looting bag");
                return;
            }
            openBank();
            Log.debug("Withdrawing looting bag");
            MyBanker.withdraw("Looting bag", 1, false);
            Waiting.waitUntil(() -> Inventory.contains("Looting bag"));
            Waiting.waitNormal(1000,100);
        }
        openBank();
        var lb = Query.inventory().nameEquals("Looting bag").findFirst().orElse(null);
        if (lb != null) {
            if (lb.click("View")) {
                Waiting.waitNormal(1000,100);
                if (isWidgetVisible(15, 8)) {
                    Waiting.waitUntil(() -> clickWidget("Deposit loot", 15, 8));
                    Waiting.waitNormal(1500,200);
                    Waiting.waitUntil(() -> clickWidget("Dismiss", 15, 10));
                    MyBanker.closeBank();
                    return;
                }
            }
        }
        MyBanker.closeBank();
        Log.debug("I don't have a looting bag");
    }

    public static void takeOffBraceCharges(){
        if (MyRevsClient.myPlayerHasTooManyChargesInBrace()) {
            Log.debug("Bracelet has too much ether. Unloading...");

            if (Equipment.contains("Bracelet of ethereum")) {
                Query.equipment().nameEquals("Bracelet of ethereum").findFirst().ifPresent(brace -> {

                    Waiting.waitUntil(1000, () -> Equipment.remove(brace.getId()) != 0);
                    Waiting.waitUntil(() -> Inventory.contains("Bracelet of ethereum"));
                    var invyBrace = Query.inventory().nameEquals("Bracelet of ethereum").findFirst().orElse(null);
                    if (invyBrace != null) {
                        Waiting.waitUntil(() -> invyBrace.click("Uncharge"));
                        Waiting.waitUntil(ChatScreen::isOpen);
                        Waiting.waitNormal(1250, 125);
                        clickWidget("Yes", 584, 1);
                        Waiting.waitUntil(() -> Inventory.contains(21820));
                    }
                });
            }else if (Inventory.contains("Bracelet of ethereum")){
                Query.inventory().nameEquals("Bracelet of ethereum").findFirst().ifPresent(brace -> {

                        Waiting.waitUntil(() -> brace.click("Uncharge"));
                        Waiting.waitUntil(ChatScreen::isOpen);
                        Waiting.waitNormal(1250, 125);
                        clickWidget("Yes", 584, 1);
                        Waiting.waitUntil(() -> Inventory.contains(21820));
                });
            }

                MyBanker.openBank();
                var amount = Query.inventory().idEquals(21820).findFirst().map(InventoryItem::getStack).orElse(0);
                MyBanker.deposit(21820, amount - 250,false);
                Waiting.waitNormal(1500, 100);
                MyBanker.closeBank();
                Waiting.wait(1000);
                Query.inventory()
                        .nameEquals("Bracelet of ethereum (uncharged)")
                        .findFirst()
                        .map(b -> Query.inventory()
                                .idEquals(21820)
                                .findFirst()
                                .map(ether -> ether.useOn(b))
                                .orElse(false));

                Waiting.wait(1000);
                Query.inventory().nameEquals("Bracelet of ethereum").findFirst().map(b -> b.click("Wear"));
            }
        }



    public static void withdrawFoodAndPots() {
        // 1. Check if you have the items we need
        // 2. Withdraw items to inventory: Prayer pot, divine ranging pot, shark, stam, ring of dueling
        // 3. restock
        // 4. Pull out
        Log.debug("Withdrawing supplies process started");
        setPlaceHolder();

        if (!isEquipmentBankTaskSatisfied()){
            Log.debug("[ERROR_LISTENER] We did not satisfy the gear setup. Trying again..");
            //withdrawGear();
            MyBanker.depositInventory();
            equipAndChargeItems();
            checkIfNeedToBuyGear();
            getEquipmentBankTask().execute();
        }

        if (!isInventoryBankTaskSatisfied()){
            Log.debug("Inventory Bank Task not satisfied..");
            Bank.depositInventory();
            checkIfNeedToRestockSupplies();
            getInventoryBankTask().execute();
        }


        // Take out our stuff
        emptyLootingBag();



            closeBank();
            Log.debug("Trying to teleport to ferox");
            if (!MyTeleporting.Dueling.FeroxEnclave.useTeleport()) {
                if (!Query.inventory().nameContains("Ring of dueling(").isAny()){
                    BankManagerRevenant.withdrawFoodAndPots();
                }
                Log.debug("Couldn't teleport to ferox.. You must be missing a ring of dueling");
            }

            var inFerox = Waiting.waitUntil(MyRevsClient::myPlayerIsInFerox);
            if (inFerox){
                Log.debug("I'm in ferox now");
                MyRevsClient.getScript().setState(State.WALKING);
            }else {
                Log.debug("Trying to teleport to ferox again..");
                MyTeleporting.Dueling.FeroxEnclave.useTeleport();
            }
    }

    private static EquipmentReq getAmulet() {
        int id = 0;

        if (Bank.contains("Salve amulet(ei)") || Equipment.contains("Salve amulet(ei)") || Inventory.contains("Salve amulet(ei)")) {
            id = 12018;
        } else if (Bank.contains("Salve amulet(i)") || Equipment.contains("Salve amulet(i)") || Inventory.contains("Salve amulet(i)")) {
            id = 12017;
        }

        if (id != 0) {
            return EquipmentReq.slot(Equipment.Slot.NECK).item(id, Amount.of(1));
        } else {
            return EquipmentReq.slot(Equipment.Slot.NECK).chargedItem("Amulet of glory", 1);
        }
    }

    private static EquipmentReq getBow(){
        /*if (Skill.RANGED.getActualLevel() >= 75) {
            return Blowpipe.equipBlowpipe(2000, Blowpipe.Dart.MITHRIL);
        }*/
        return EquipmentReq.slot(Equipment.Slot.WEAPON).item(22550, Amount.of(1));
    }

    public static BankTask getEquipmentBankTask() {
        if (equipmentBankTask == null) {
            Log.debug("equipment task");
            openBank();
            setPlaceHolder();


            if (!MyRevsClient.myPlayerHas40Defence()) {
                equipmentBankTask = BankTask.builder()
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.RING).chargedItem("Ring of wealth", 1))
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.HEAD).item(1169, Amount.of(1)))
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.BODY).item(1129, Amount.of(1)))
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.LEGS).item(2497, Amount.of(1)))
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.FEET).item(1061, Amount.of(1)))
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.CAPE).item(12273, Amount.of(1)))
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.HANDS).item(21816, Amount.of(1)))
                        .addEquipmentItem(getBow())
                        .addEquipmentItem(BankManagerRevenant::getAmulet)
                        .build();
            }else {
                equipmentBankTask = BankTask.builder()
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.RING).chargedItem("Ring of wealth", 1))
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.HEAD).item(6326, Amount.of(1))) // snakeskin helm
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.BODY).item(2503, Amount.of(1))) // black d hide body
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.LEGS).item(2497, Amount.of(1))) // black d hide chaps
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.FEET).item(6328, Amount.of(1))) // snakeskin boots
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.CAPE).item(12273, Amount.of(1)))
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.HANDS).item(21816, Amount.of(1)))
                        .addEquipmentItem(getBow())
                        .addEquipmentItem(BankManagerRevenant::getAmulet)
                        .build();
            }



        }

        return equipmentBankTask;
    }

    private static boolean useEther(boolean bow) {
        int[] targetIds = bow ? new int[]{22550, 22547} : new int[]{21816, 21817};
        if (!Inventory.contains("Revenant ether")) return false;
        closeBank();
        if (!Inventory.contains(targetIds)) {
            var optionalItem = Query.equipment().idEquals(targetIds).findFirst();
            if (optionalItem.isEmpty()) {
                Log.warn("Tried to use ether on " + (bow ? "bow" : "bracelet") + " but do not have one withdrawn");
                return false;
            } else {
                var item = optionalItem.get();
                Equipment.remove(item.getId());
                Waiting.waitUntil(2000, () -> Inventory.contains(item.getId()));
            }
        }
        return Query.inventory()
                .idEquals(targetIds)
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

    public static boolean hasEnoughEther(int amount) {
        return Query.bank().idEquals(21820).findFirst().map(Stackable::getStack).orElse(0) >= amount;
    }


    private static boolean withdrawEther(int amount) {
        // Go buy if we dont have enoug ether
        int inventoryAmount = Inventory.getCount(21820);
        int shortage = amount - inventoryAmount;

        if (shortage == 0) return true;
        if (shortage < 0) {
            return MyBanker.deposit(21820, Math.abs(shortage), false);
        } else {
            if (!hasEnoughEther(amount)) {
                GrandExchangeRevManager.sellLoot();
                GrandExchangeRevManager.buyFromBank(21820, 4000);
            }
            if (Inventory.getAll().size() > 25){
                MyBanker.openBank();
                MyBanker.depositInventory();
            }
            return MyBanker.withdraw(21820, amount, false);
        }

    }

    private static boolean inventoryContainsEther(int amount) {
        return Inventory.getCount("Revenant ether") >= amount;
    }

    private static boolean equipAndCharge(boolean bow) {
        int etherGoal = bow ? 300 : 100;
        if (!isChargedItemWithdrawn(bow)) {
            if (!withdrawCharged(bow)) {
                Log.warn("Failed to withdraw bow, may need to buy?");
                return false;
            }
            if (bow && equipmentContainsCharged(true) || inventoryContainsCharged(true)){
                Log.debug("I have a charged bow equip or in invy");
                etherGoal = bow ? 300 : 100;
            }else {
                etherGoal = bow ? 1300 : 100;
            }

        }

        int charges = checkCharges(bow);

        Log.debug("My charges: " + charges);
        if (charges < etherGoal) {
            int shortage = etherGoal - charges;
            withdrawEther(shortage);
            if (useEther(bow)) {
                // Something went wrong. Couldn't use ether on
                Log.debug("Something went wrong.. maybe out of ether... Couldn't use ether on bracelet");
            }
        }else if(charges > etherGoal && !bow){
            takeOffBraceCharges();
        }

        int equipId = bow ? 22550 : 21817;

        Query.inventory().idEquals(equipId).findFirst().ifPresent(Equipment::equip);

        return Waiting.waitUntil(2000, () -> Equipment.contains(equipId));
    }

    private static int checkCharges(boolean bow) {
        return bow ? EquipmentManager.checkBowCharges() : EquipmentManager.checkBraceletCharges();
    }

    private static boolean withdrawCharged(boolean bow) {
        if (isChargedItemWithdrawn(bow)) return true;
        int chargedId = bow ? 22550 : 21816;
        int unchargedId = bow ? 22547 : 21817;
        if (!equipmentContainsCharged(bow) && !equipmentContainsUncharged(bow)) {
            MyBanker.openBank();
            if (Bank.contains(chargedId)) {
                return MyBanker.withdraw(chargedId, 1, false);
            } else if (Bank.contains(unchargedId)) {
                return MyBanker.withdraw(unchargedId, 1, false);
            } else {
                Log.warn("Could not withdraw " + (bow ? "bow" : "bracelet"));
                return false;
            }
        }
        return false;
    }

    private static boolean inventoryContainsCharged(boolean bow) {
        return bow
                ? Inventory.contains(22550)
                : Inventory.contains(21816);
    }

    private static boolean equipmentContainsCharged(boolean bow) {
        return bow
                ? Equipment.contains(22550)
                : Equipment.contains(21816);
    }

    private static boolean equipmentContainsUncharged(boolean bow) {
        return bow
                ? Equipment.contains(22547)
                : Equipment.contains(21817);
    }

    private static boolean isChargedItemWithdrawn(boolean bow) {
        return inventoryContainsCharged(bow) || equipmentContainsCharged(bow);
    }


    public static void withdrawGear() {
        /*if (withdrawGearAttempts.incrementAndGet() > 3) {
            Log.error("Failed withdrawing gear three times");
            throw new RuntimeException("Failed withdrawing gear three times");
        }*/

        Log.debug("Started withdrawing gear process");
        if (MyRevsClient.myPlayerIsInFerox()){
            GlobalWalking.walkTo(new WorldTile(3133, 3628, 0)); // bank spot at ferox
        }
        while (!MyBanker.openBank()){
            Log.debug("Couldn't enter the bank. Trying again..");
            MyBanker.openBank();
            Waiting.wait(3000);
        }

        Log.debug("Entered bank");
        Waiting.waitNormal(2000, 300);
        Log.debug("Checking weapon charges");
        equipAndChargeItems();

        if (!isEquipmentBankTaskSatisfied()) {
            Log.debug("Equipment task not satisfied. Checking if we need to buy gear..");
            checkIfNeedToBuyGear();
            getEquipmentBankTask().execute();

        }/* else {
            Log.debug("Checking brace and bow charges");
            EquipmentManager.checkBraceletCharges();
            EquipmentManager.checkBowCharges();
        }*/

        if (!getEquipmentBankTask().isSatisfied()) {
            openBank();
            MyBanker.depositAll();
            Log.debug("Equipment not satisfied. Trying again");
            equipAndChargeItems();
            getEquipmentBankTask().execute();

        }

        withdrawFoodAndPots();

        // if need to buy anything, can do it here or handle it here anyways
    }

    public static BankTask getInventoryBankTask() {
        return BankTask.builder()
                .addInvItem(24598, Amount.of(4)) // Blighted super restore
                .addInvItem(6685, Amount.of(3)) // brew
                .addInvItem(2550, Amount.of(1)) // recoil
                .addInvItem(385, Amount.of(15)) // shark
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


        for (var item : MyRevsClient.myPlayerHas40Defence() ? EquipmentManager.getDefenceGear() : EquipmentManager.getBasicGear()) {

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

        if (!Query.bank().nameEquals("Amulet of glory(6)").isAny()){
            if (isUsingGlory){
                Log.debug("Out of glories. Adding to buy list!");
                itemsToBuy.add("Amulet of glory(6)");
            }
        }


        if (itemsToBuy.size() != 0) {
            MyExchange.walkToGrandExchange();
            GrandExchangeRevManager.sellLoot();

            GrandExchangeRevManager.restockFromBank(itemsToBuy);
            Bank.depositInventory();
            Waiting.waitUntil(Inventory::isEmpty);

        }else {
            Log.debug("No items to buy");
        }

    }
    /*
    private static boolean inventoryContainsUncharged(boolean bow) {
        return bow
                ? Inventory.contains(22547)
                : Inventory.contains(21817);
    }/*

}
     */
}