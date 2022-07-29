package scripts;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.EquipmentItem;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;

import java.util.ArrayList;
import java.util.List;

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
        }
        Waiting.wait(3500);
        withdrawPVMInventory();
    }

    public static void withdrawPVMInventory() {
        var shouldDecant = false;
        List<String> stackablesToBuy = new ArrayList<>();
        var hasWealthCharges = true;
        if (!Query.equipment().nameContains("Ring of wealth (").isAny()) {
            hasWealthCharges = false;
            if (Inventory.isFull() && Query.inventory().nameEquals("Shark").isAny()){
                Query.inventory().nameEquals("Sarhk").findFirst().map(InventoryItem::click);
                Waiting.wait(1500);
            }
            Equipment.remove("Ring of wealth");
        }
        EquipmentManager.checkCharges();
        Waiting.wait(3000);
        //Log.info(MyRevsClient.myPlayerHasEnoughChargesInBow());

        if (!MyRevsClient.myPlayerHasEnoughChargesInBow()) {
            //Log.info(EquipmentManager.getBowCharges());
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
            } else { // REvenant maldictus
                Bank.withdrawAll("Coins");
                Bank.close();
                GrandExchangeRevManager.openGE();
                GrandExchangeRevManager.buy("Revenant ether");
                GrandExchange.close();
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

        openBank();
        var lb = Query.inventory().nameEquals("Looting bag").findFirst().orElse(null);
        if (lb != null){
            lb.click("View");
                    if (MyRevsClient.isWidgetVisible(15, 3)) {
                        Waiting.waitUntil(() -> MyRevsClient.clickWidget("Deposit loot", 15, 8));
                        Waiting.waitUntil(() -> MyRevsClient.clickWidget("Dismiss", 15, 10));
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
            Bank.withdraw(staminapot, 1);
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
            Bank.withdraw(shark, 15);
            Waiting.wait(1500);
            ensureItemIsWithdrawn(shark.getName(), 15);
        }else {
            stackablesToBuy.add("Shark");
        }

        var dueling = Query.bank().nameContains("Ring of dueling(").findFirst().orElse(null);
        if (dueling != null) {
            Bank.withdraw(dueling.getName(), 1);
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
        }


        if (shouldDecant){
            DecantManager.decantPotionsFromBank();
            withdrawPVMInventory();
        }

        if (stackablesToBuy.size() != 0){
            if (!GrandExchange.isNearby()){
                RevenantScript.state = State.SELLLOOT;
                GlobalWalking.walkTo(new WorldTile(3164, 3484, 0));
            }
            openBank();
            GrandExchangeRevManager.restockFromBank(stackablesToBuy, new ArrayList<>());
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
        // BASIC GEAR
        Bank.depositInventory();
        Bank.depositEquipment();
        Waiting.wait(2000);
        itemsToBuy = new ArrayList<>();
        List<String> stackables = new ArrayList<>();
        for (var item : EquipmentManager.getBasicGear()) {
            if (item.equals("Craw's bow")){
                if (Query.bank().nameEquals("Craw's bow (u)").isAny()){
                    Bank.withdraw("Craw's bow (u)", 1);
                    Waiting.waitUntil(2000, () -> Inventory.contains("Craw's bow (u)"));
                    ensureItemIsWithdrawn("Craw's bow (u)", 1);

                    continue;
                }
            }
            if (!Query.bank().nameEquals(item).isAny()) {
                //Log.info(item);
                itemsToBuy.add(item);
            }



            if (item.equals("Amulet of glory(6)")){
                if (Query.bank().nameEquals("Salve amulet(i)").isAny()){
                    Bank.withdraw("Salve amulet(i)", 1);
                    Waiting.waitUntil(2000, () -> Inventory.contains(item));
                    ensureItemIsWithdrawn(item, 1);
                }else {
                    Bank.withdraw(item, 1);
                    Waiting.waitUntil(2000, () -> Inventory.contains(item));
                    ensureItemIsWithdrawn(item, 1);
                }
            }
            if (!Inventory.contains(item) && !EquipmentManager.equipmentContains(item)) {
                Bank.withdraw(item, 1);
                Waiting.waitUntil(2000, () -> Inventory.contains(item));
                ensureItemIsWithdrawn(item, 1);
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

        /*if (Bank.contains("Magic shortbow")) {
            if (!Inventory.contains("Magic shortbow") && !EquipmentManager.equipmentContains("Magic shortbow")) {
                Bank.withdraw("Magic shortbow", 1);
                Waiting.waitUntil(2000, () -> Inventory.contains("Magic shortbow"));
                ensureItemIsWithdrawn("Magic shortbow", 1);
            }
        }

        if (Bank.contains("Rune arrow")) {
            if (!Inventory.contains("Rune arrow") && !EquipmentManager.equipmentContains("Rune arrow")) {
                Bank.withdraw(892, 100);
                Waiting.waitUntil(2000, () -> Inventory.contains("Rune arrow"));
                ensureItemIsWithdrawn("Rune arrow", 100);
            }
        }*/
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

                Bank.withdraw("Bracelet of ethereum", 1);
                Waiting.wait(1000);
                ensureItemIsWithdrawn("Bracelet of ethereum", 1);
                Waiting.waitUntil(2000, () -> Inventory.contains("Bracelet of ethereum"));

            }
        } else if (Bank.contains("Bracelet of ethereum (uncharged)")) {
            if (!Inventory.contains("Bracelet of ethereum (uncharged)") && !EquipmentManager.equipmentContains("Bracelet of ethereum (uncharged)")) {
                Bank.withdraw("Bracelet of ethereum (uncharged)", 1);
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
                RevenantScript.state = State.SELLLOOT;
                GlobalWalking.walkTo(new WorldTile(3164, 3484, 0));
            }
            openBank();
            GrandExchangeRevManager.restockFromBank(itemsToBuy, stackables);
        }

        if (Bank.isOpen()) {
            Bank.close();
        }

        EquipmentManager.equipGear();

        Waiting.wait(2000);
        withdrawPVMInventory();

    }

    public static boolean inventoryContains(String name) {
        return Query.inventory().nameContains(name).isAny();
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
        withdrawGear();
        Bank.depositInventory();
    }

}
