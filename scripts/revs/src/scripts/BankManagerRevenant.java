package scripts;

import net.sourceforge.jdistlib.InvNormal;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.interfaces.Clickable;
import org.tribot.script.sdk.interfaces.Identifiable;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.tasks.Amount;
import org.tribot.script.sdk.tasks.BankTask;
import org.tribot.script.sdk.tasks.EquipmentReq;
import org.tribot.script.sdk.tasks.ItemReq;
import org.tribot.script.sdk.types.EquipmentItem;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class BankManagerRevenant {
    public static List<String> itemsToBuy = new ArrayList<>();

    public static void openBank() {
        if (!Bank.isNearby()) {
            GlobalWalking.walkToBank();
        }
        if (!Bank.isOpen()) {
            Bank.open();
        }
        Waiting.wait(1000);
    }

    public static void bankLoot() {
        if (!MyRevsClient.myPlayerIsInGE()){
            Query.equipment().nameContains("Ring of wealth (").findFirst().map(ring -> ring.click("Grand exchange"));
            Waiting.waitUntil(MyRevsClient::myPlayerIsInGE);
        }
        returnFromTrip();
        //withdrawPVMInventory();
    }

    public static void checkIfWeHaveEmblemDrop(){
        List<String> valueAbles = new ArrayList<>(Arrays.asList("Ancient relic", "Ancient effigy", "Ancient medallion"));
        for (var item : valueAbles){
            if (Query.bank().nameEquals(item).isAny()){
                Log.debug("We have: " + item);
                GrandExchangeRevManager.sellLoot();
                return;
            }
        }
    }

    public static void returnFromTrip(){
        EquipmentManager.checkCharges();
        BankManagerRevenant.openBank();
        Waiting.waitUntil(Bank::isOpen);
        equipNewWealthIfNeeded();
        chargeBraceletOrBowIfNeeded();
        checkIfWeHaveEmblemDrop();
        withdrawFoodAndPots();

    }

    public static void equipNewWealthIfNeeded(){
        if (!EquipmentManager.hasWealthCharges()){
            var ring = Query.bank().nameContains("Ring of wealth (").findFirst().orElse(null);
            if (ring != null){
                BankTask.builder()
                        .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.RING).chargedItem("Ring of wealth", 1)).build().execute();
            }else {
                Log.debug("Out of ring of wealths. Selling loot to buy more!");
                GrandExchangeRevManager.sellLoot();
                GrandExchangeRevManager.restockFromBank(new ArrayList<>(Arrays.asList("Ring of wealth (5)")));
            }
        }
    }

    public static void chargeBraceletOrBowIfNeeded(){
        // Check if we have enough ether in the bank
        if (!MyRevsClient.myPlayerHasEnoughChargesInBow()) {
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
        if (MyRevsClient.myPlayerHasTooManyChargesInBrace()){
            Log.debug("Bracelet has too much ether. Unloading...");
            if (Bank.isOpen()){
                Bank.close();
            }
            var brace = Query.equipment().nameEquals("Bracelet of ethereum").findFirst().orElse(null);
            if (brace != null){
                Waiting.waitUntil(1000, () -> Equipment.remove(brace.getId()) != 0);
                Waiting.waitUntil(() -> Inventory.contains("Bracelet of ethereum"));
                var invyBrace = Query.inventory().nameEquals("Bracelet of ethereum").findFirst().orElse(null);
                if (invyBrace != null){
                    Waiting.waitUntil(() -> invyBrace.click("Uncharge"));
                    Waiting.waitUntil(ChatScreen::isOpen);
                    if (MyRevsClient.isWidgetVisible(584, 0)){
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



    public static void withdrawFoodAndPots(){
        // 1. Check if you have the items we need
        // 2. Withdraw items to inventory: Prayer pot, divine ranging pot, shark, stam, ring of dueling
        // 3. restock
        // 4. Pull out
        openBank();
        Bank.depositInventory();
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
        if (itemsToBuy.size() != 0){
            itemsToBuy.forEach(Log::info);
            if (!GrandExchange.isNearby()){
                GlobalWalking.walkTo(new WorldTile(3164, 3484, 0));
            }
            Log.debug("I'm out of supplies. Selling loot and buying more.");
            GrandExchangeRevManager.sellLoot();
            openBank();
            GrandExchangeRevManager.restockFromBank(itemsToBuy);
            Bank.depositInventory();
            withdrawGears();
        }

        // Take out our stuff



        BankTask.builder()
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
                .build().execute();

        openBank();
        var lb = Query.inventory().nameEquals("Looting bag").findFirst().orElse(null);
        if (lb != null){
            if (lb.click("View")){
                if (MyRevsClient.isWidgetVisible(15, 3)) {
                    Waiting.waitUntil(() -> MyRevsClient.clickWidget("Deposit loot", 15, 8));
                    Waiting.waitUntil(() -> MyRevsClient.clickWidget("Dismiss", 15, 10));
                    Bank.close();
                }
            }
        }
        Bank.close();

        if (Equipment.getAll().size() != 9){
            Log.debug("I dont have 9 items on");
            withdrawGears();
        }

        Log.debug("End of withdrawpotsandfood");

        RevenantScript.state = State.WALKING;

    }

    public static void withdrawGears(){

        openBank();
        Waiting.waitUntil(100000, Bank::isOpen);
        Waiting.waitUntil(100000, Bank::depositInventory);
        Waiting.waitUntil(100000, Bank::depositEquipment);

        Waiting.wait(5000);
        var hasSalve = false;
        int salve = 0;
        EquipmentReq craw = null;
        EquipmentReq bracelet = null;
        List<String> itemsToBuy = new ArrayList<>();


            for (var item: EquipmentManager.getBasicGear()){

                if (item.equals("Craw's bow") || item.equals("Salve amulet(i)") || item.equals("Salve amulet(ei)")){
                    continue;
                }
                if (!Query.bank().nameEquals(item).isAny()) {
                    Log.debug("We don't have: " + item + ". Added to list");
                    itemsToBuy.add(item);
                }

                if (item.equals("Amulet of glory(6)")){
                    if (Query.bank().nameEquals("Salve amulet(ei)").isAny()){
                        hasSalve = true;
                        salve = 25278;
                    }else if(Query.bank().nameEquals("Salve amulet(i)").isAny()){
                        if (Query.bank().nameEquals("Salve amulet(i)").isAny()){
                            hasSalve = true;
                            salve = 12017;
                        }
                    }
                }
            }
            if (!Query.bank().nameContains("Ring of wealth (").isAny()){
                Log.debug("We're out of wealths. Added to list.");
                itemsToBuy.add("Ring of wealth (5)");
            }

            if (!Query.bank().nameEquals("Bracelet of ethereum").isAny() && !Query.bank().nameEquals("Bracelet of ethereum (uncharged)").isAny()){
                Log.debug("We're out of bracelets. Added to list.");
                itemsToBuy.add("Bracelet of ethereum (uncharged)");
            }
        Waiting.wait(1000);

        if (Bank.contains(22550)){
            craw = EquipmentReq.slot(Equipment.Slot.WEAPON).item(22550, Amount.of(1));
        }else if (Bank.contains(22547)){
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

        Waiting.wait(1000);

        if (Bank.contains("Bracelet of ethereum")) {
            bracelet = EquipmentReq.slot(Equipment.Slot.HANDS).item(21816, Amount.of(1));
        } else if (Bank.contains("Bracelet of ethereum (uncharged)")) {
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



            if (itemsToBuy.size() != 0){
                if (!GrandExchange.isNearby()){
                    GlobalWalking.walkTo(new WorldTile(3164, 3484, 0));
                }
               GrandExchangeRevManager.sellLoot();
                openBank();
                GrandExchangeRevManager.restockFromBank(itemsToBuy);
                Bank.depositInventory();
                withdrawGears();
            }

        var glory = EquipmentReq.slot(Equipment.Slot.NECK).chargedItem("Amulet of glory", 1);
        var sal =  EquipmentReq.slot(Equipment.Slot.NECK).item(salve, Amount.of(1));

        BankTask.builder()
                .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.RING).chargedItem("Ring of wealth", 1))
                .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.HEAD).item(1169, Amount.of(1)))
                .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.BODY).item(1129, Amount.of(1)))
                .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.LEGS).item(2497, Amount.of(1)))
                .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.FEET).item(1061, Amount.of(1)))
                .addEquipmentItem(EquipmentReq.slot(Equipment.Slot.CAPE).item(12273, Amount.of(1)))
                .addEquipmentItem(hasSalve ? sal : glory)
                .addEquipmentItem(bracelet)
                .addEquipmentItem(craw).build().execute();

        withdrawFoodAndPots();
    }



    public static void withdrawPVMInventory() {
        var shouldDecant = false;
        List<String> stackablesToBuy = new ArrayList<>();
        var hasWealthCharges = true;
        if (!Query.equipment().nameContains("Ring of wealth (").isAny()) {
            hasWealthCharges = false;
            if (Inventory.isFull() && Query.inventory().nameEquals("Shark").isAny()){
                Query.inventory().nameEquals("Shark").findFirst().map(InventoryItem::click);
                Waiting.wait(1500);
            }
            Equipment.remove("Ring of wealth");
        }
        EquipmentManager.checkCharges();
        Waiting.wait(3000);


        openBank();
        var lb = Query.inventory().nameEquals("Looting bag").findFirst().orElse(null);
        if (lb != null){
            if (lb.click("View")){
                if (MyRevsClient.isWidgetVisible(15, 3)) {
                    Waiting.waitUntil(() -> MyRevsClient.clickWidget("Deposit loot", 15, 8));
                    Waiting.waitUntil(() -> MyRevsClient.clickWidget("Dismiss", 15, 10));
                }
            }

        }



        Bank.depositInventory();
        if (!hasWealthCharges) {
            var wealth = Query.bank().nameContains("Ring of wealth (").findFirst().orElse(null);

            if (wealth != null) {
                Waiting.waitUntil(2000, () -> Bank.withdraw(wealth, 1));
                Waiting.wait(500);
                Waiting.waitUntil(2000, Bank::close);
                Waiting.wait(500);
                Query.inventory().nameContains("Ring of wealth (").findFirst().map(InventoryItem::click);
                openBank();
            }else {
                Bank.withdrawAll("Coins");
                stackablesToBuy.add("Ring of wealth (5)");
            }

        }

        var prayerpot = Query.bank().idEquals(2434).findFirst().orElse(null);
        if (prayerpot != null) {
            if (Bank.getCount(prayerpot.getName()) < 5){
                stackablesToBuy.add("Prayer potion(4)");
            }
            Bank.withdraw(prayerpot, 5);
            Waiting.wait(1500);
            ensureItemIsWithdrawn(prayerpot.getName(), 5);
        }else {
            if (!DecantManager.hasDecanted){
                shouldDecant = true;
            }else {
                stackablesToBuy.add("Prayer potion(4)");
            }

        }
        var rangingpot = Query.bank().nameContains("Divine ranging potion").findFirst().orElse(null);
        if (rangingpot != null) {
            Bank.withdraw(rangingpot, 1);
            Waiting.wait(1500);
            ensureItemIsWithdrawn(rangingpot.getName(), 1);
        }else {
            if (!DecantManager.hasDecanted){
                shouldDecant = true;
            }else {
                stackablesToBuy.add("Divine ranging potion(4)");
            }
        }

        var staminapot = Query.bank().nameContains("Stamina potion").findFirst().orElse(null);
        if (staminapot != null) {
            Waiting.waitUntil(() -> Bank.withdraw(staminapot, 1));
            Waiting.wait(1500);
            ensureItemIsWithdrawn(staminapot.getName(), 1);
        }else {
            if (!DecantManager.hasDecanted){
                shouldDecant = true;
            }else {
                stackablesToBuy.add("Stamina potion(4)");
            }
        }
        var shark = Query.bank().nameEquals("Shark").findFirst().orElse(null);
        if (shark != null) {
            if (Bank.getCount("Shark") < 15){
                stackablesToBuy.add("Shark");
            }
            Waiting.waitUntil(() -> Bank.withdraw(shark, 15));
            Waiting.wait(1500);
            ensureItemIsWithdrawn(shark.getName(), 15);
        }else {
            stackablesToBuy.add("Shark");
        }

        var dueling = Query.bank().nameContains("Ring of dueling(").findFirst().orElse(null);
        if (dueling != null) {
            Waiting.waitUntil(() -> Bank.withdraw(dueling.getName(), 1));
            Waiting.wait(1500);
            ensureItemIsWithdrawn(dueling.getName(), 1);
        }else {
            stackablesToBuy.add("Ring of dueling(8)");
        }

        var lootingBag = Query.bank().nameContains("Looting bag").findFirst().orElse(null);
        if (lootingBag != null) {
            Bank.withdraw(lootingBag.getName(), 1);
            Waiting.wait(1500);
            ensureItemIsWithdrawn(lootingBag.getName(), 1);
            Waiting.waitUntil(() -> Inventory.contains("Looting bag"));


        }


        if (shouldDecant){
            DecantManager.decantPotionsFromBank();
            withdrawPVMInventory();
        }

        if (stackablesToBuy.size() != 0){
            if (!GrandExchange.isNearby()){
                GlobalWalking.walkTo(new WorldTile(3164, 3484, 0));
            }
            GrandExchangeRevManager.sellLoot();
            openBank();
            GrandExchangeRevManager.restockFromBank(stackablesToBuy);
        }

        if (Bank.isOpen()) {
            Bank.close();
        }


        var bag = Query.inventory().nameEquals("Looting bag").findFirst().orElse(null);
        if (bag != null) {
            if (bag.getId() == 11941) {
                bag.click();
            }
        }
        WorldManager.hopToRandomMemberWorldWithRequirements();
        RevenantScript.state = State.WALKING;
    }

    public static void ensureItemIsWithdrawn(String itemName, int amount) {
        if (!Inventory.contains(itemName)) {
            Bank.withdraw(itemName, amount);
        }else {
            if (Inventory.getCount(itemName) != amount) {
                Bank.depositAll(itemName);
                Bank.withdraw(itemName, amount);
            }
        }
    }

    public static void withdrawGear() {
        openBank();

        setPlaceHolder();
        Waiting.waitUntil(Bank::depositInventory);
        Waiting.waitUntil(Bank::depositEquipment);
        Waiting.wait(2000);

        itemsToBuy = new ArrayList<>();


        for (var item : EquipmentManager.getBasicGear()) {
            if (item.equals("Craw's bow") || item.equals("Salve amulet(i)") || item.equals("Salve amulet(ei)")){
                    continue;
            }
            if (!Query.bank().nameEquals(item).isAny()) {
                itemsToBuy.add(item);
            }

            if (item.equals("Amulet of glory(6)")){
                if (Query.bank().nameEquals("Salve amulet(ei)").isAny()){
                    Bank.withdraw("Salve amulet(ei)", 1);
                    Waiting.waitUntil(2000, () -> Inventory.contains("Salve amulet(ei)"));
                    ensureItemIsWithdrawn("Salve amulet(i)", 1);
                    continue;
                }else if(Query.bank().nameEquals("Salve amulet(i)").isAny()){
                    if (Query.bank().nameEquals("Salve amulet(i)").isAny()){
                        Bank.withdraw("Salve amulet(i)", 1);
                        Waiting.waitUntil(2000, () -> Inventory.contains("Salve amulet(i)"));
                        ensureItemIsWithdrawn("Salve amulet(i)", 1);
                        continue;
                    }
                } else {
                    Bank.withdraw(item, 1);
                    Waiting.waitUntil(2000, () -> Inventory.contains(item));
                    ensureItemIsWithdrawn(item, 1);
                }
            }
            if (!Inventory.contains(item) && !EquipmentManager.equipmentContains(item)) {
                Waiting.waitUntil(() -> Bank.withdraw(item, 1));
                //ensureItemIsWithdrawn(item, 1);
            }

        }

        // WEALTH
        if (Query.bank().nameContains("Ring of wealth (").isAny()) {
            if (!inventoryContains("Ring of wealth (") && !EquipmentManager.equipmentContains("Ring of wealth (")) {
                var wealth = Query.bank().nameContains("Ring of wealth (").findRandom().orElse(null);
                if (wealth != null) {
                    Bank.withdraw(wealth, 1);
                    Waiting.waitUntil(2000, () -> Inventory.contains(wealth.getName()));
                    ensureItemIsWithdrawn(wealth.getName(), 1);
                }
            }
        } else {
            itemsToBuy.add("Ring of wealth (5)");
        }


        // CRAWS
        if (Bank.contains("Craw's bow")) {
            if (!Inventory.contains("Craw's bow") && !EquipmentManager.equipmentContains("Craw's bow")) {
                Bank.withdraw("Craw's bow", 1);
                ensureItemIsWithdrawn("Craw's bow", 1);
            } else {
                Bank.close();
                EquipmentManager.checkCharges();
                Waiting.wait(2000);
                if (EquipmentManager.getBraceCharges() < 150) {
                    Bank.withdraw("Revenant ether", EquipmentManager.getBraceCharges() + 150);
                    Bank.close();
                    Query.inventory()
                            .nameEquals("Bracelet of ethereum (uncharged)")
                            .findFirst()
                            .map(brace -> Query.inventory()
                                    .nameEquals("Revenant ether")
                                    .findFirst()
                                    .map(ether -> ether.useOn(brace))
                                    .orElse(false));
                }
                if (EquipmentManager.getBowCharges() < 250) {
                    Bank.withdraw("Revenant ether", 600);
                    Bank.close();
                    Query.inventory()
                            .nameEquals("Bracelet of ethereum (uncharged)")
                            .findFirst()
                            .map(brace -> Query.inventory()
                                    .nameEquals("Revenant ether")
                                    .findFirst()
                                    .map(ether -> ether.useOn(brace))
                                    .orElse(false));
                }
                openBank();
            }
        } else if (Bank.contains("Craw's bow (u)")) {
            if (!Inventory.contains("Craw's bow (u)") && !EquipmentManager.equipmentContains("Craw's bow (u)")) {
                Bank.withdraw("Craw's bow (u)", 1);
                if (Bank.getCount(21820) >= 1750 + 250) {
                    Bank.withdraw(21820, 1750);
                    Waiting.wait(1000);
                    ensureItemIsWithdrawn("Revenant ether", 1750);
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
                } else {
                    GrandExchangeRevManager.buyFromBank(21820, 4000);
                }
            }
        }

        // BRACELET
        if (Bank.contains("Bracelet of ethereum")) {
            if (!Inventory.contains("Bracelet of ethereum") && !EquipmentManager.equipmentContains("Bracelet of ethereum")) {

                Waiting.waitUntil(() -> Bank.withdraw("Bracelet of ethereum", 1));
                ensureItemIsWithdrawn("Bracelet of ethereum", 1);
                Waiting.waitUntil(2000, () -> Inventory.contains("Bracelet of ethereum"));

            }
        } else if (Bank.contains("Bracelet of ethereum (uncharged)")) {
            if (!Inventory.contains("Bracelet of ethereum (uncharged)") && !EquipmentManager.equipmentContains("Bracelet of ethereum (uncharged)")) {
                Waiting.waitUntil(() -> Bank.withdraw("Bracelet of ethereum (uncharged)", 1));

                if (Bank.getCount(21820) >= 250) {
                    Bank.withdraw(21820, 250);
                    Waiting.wait(1000);
                    ensureItemIsWithdrawn("Revenant ether", 250);
                    Waiting.waitUntil(2000, () -> Inventory.contains(21820));

                }
                Bank.close();
                Waiting.waitUntil(2000, () -> Query.inventory().idEquals(21820).isAny());
                Query.inventory().idEquals(21820).findFirst().map(ether -> ether.click("Use"));
                Query.inventory().nameEquals("Bracelet of ethereum (uncharged)").findFirst().map(InventoryItem::click);
            }
        } else {
            itemsToBuy.add("Bracelet of ethereum (uncharged)");
        }



        if (itemsToBuy.size() != 0) {
            if (!GrandExchange.isNearby()){
                GlobalWalking.walkTo(new WorldTile(3164, 3484, 0));
            }
            openBank();
            var gp = Query.bank().idEquals(995).findFirst().orElse(null);
            assert gp != null;
            if (gp.getStack() < 1000000){
                GrandExchangeRevManager.sellLoot();
            }
            GrandExchangeRevManager.restockFromBank(itemsToBuy);
        }

        if (Bank.isOpen()) {
            Bank.close();
        }

        EquipmentManager.equipGear();

        Waiting.wait(2000);
        withdrawFoodAndPots();

    }

    public static boolean inventoryContains(String name) {
        return Query.inventory().nameContains(name).isAny();
    }


    public static void setPlaceHolder(){
            if (!BankSettings.isPlaceholdersEnabled()){
                Waiting.waitUntil(() -> MyRevsClient.clickWidget("Enable", 12, 38));
            }
    }


    public static boolean bankContains(String name) {
        return Query.bank().nameContains(name).isAny();
    }

    public static void withdrawItemByName(String itemName) {
        openBank();
        var item = Query.bank().nameContains(itemName).findFirst().orElse(null);
        if (item != null) {
            Bank.withdraw(item, 1);
            Waiting.wait(2000);
        }
    }


    public static void init() {
        withdrawGears();
    }

}
