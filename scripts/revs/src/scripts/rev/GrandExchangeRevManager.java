package scripts.rev;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.GrandExchangeOffer;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import scripts.api.MyBanker;
import scripts.api.MyExchange;
import scripts.api.MyScriptVariables;
import scripts.api.MyWorldHopper;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static scripts.api.MyBanker.closeBank;
import static scripts.api.MyBanker.openBank;
import static scripts.api.MyClient.clickWidget;
import static scripts.api.utility.Utility.distinctBy;

public class GrandExchangeRevManager {

    private static boolean shouldRepeat = false;
    private static boolean inFirstTrade = false;

    public static List<InventoryItem> getAllSellItems() {
        return Query.inventory().filter(distinctBy(InventoryItem::getIndex)).filter(item ->
                        item.getId() != 995 &&
                        item.getId() != 22547 &&
                        !item.getName().contains("Blighted")).distinctById().toList();
    }


    public static void sellLoot() {
        Log.debug("Trying to sell loot");
        closeBank();
        MyExchange.walkToGrandExchange();
        if(!openBank()){
            openBank();
        }
        if (!BankSettings.isNoteEnabled()) {
            BankSettings.setNoteEnabled(true);
        }
        Bank.depositInventory();
        var isEmpty = Waiting.waitUntil(Inventory::isEmpty);
        if (!isEmpty) {
            Log.debug("Couldn't empty. Trying again..");
            Waiting.waitUntil(Inventory::isEmpty);
        }
        MyBanker.withdraw("Coins", 2147000000, false);
        AtomicInteger itemsToSell = new AtomicInteger();
        for (var item : LootingManager.getLootToPickUp()) {
            if (item.equals("Looting bag") || item.equals("Coins") || item.equals("Craw's bow (u)") || item.contains("Blighted") || MyRevsClient.getScript().isSkulledScript() && item.equals("Amulet of avarice")) {
                continue;
            }

            if (Inventory.isFull()) {
                shouldRepeat = true;
                break;
            }

            if (item.equals("Bracelet of ethereum (uncharged)")) {
                if (!Bank.contains(item)) {
                    continue;
                }
                if (Bank.getCount(item) <= 5 && Bank.contains(item)) {
                    continue;
                }
                Log.warn("Pulling out bracelet");
                MyBanker.withdraw(item, Bank.getCount(item) - 5, true);
                Waiting.waitUntil(() -> Inventory.contains(item));
                itemsToSell.getAndIncrement();
                continue;
            }

            if (Query.bank().nameEquals(item).isAny()) {
                itemsToSell.getAndIncrement();
                MyBanker.withdraw(item, 10000000, true);
            }
        }

        Log.debug("Items to sell: " + itemsToSell.get());

        if (itemsToSell.get() == 0) {
            Log.debug("No items to sell");
            openBank();
            MyBanker.depositAll();
            mule();
            return;
        }



        MyBanker.closeBank();
        MyExchange.openExchange();

        for (var item : getAllSellItems()) {
            // Will wait until the offer shows up in the GE.

            boolean successfullyPosted = false;
            int attempts = 0;

            while (!successfullyPosted && attempts < 5) {
                if (!MyExchange.isExchangeOpen()) {
                    MyExchange.openExchange();
                }
                attempts++;
                successfullyPosted = MyExchange.createGrandExchangeSellOrder(item);
                // Check if GE is full
                if (MyExchange.isGrandExchangeSlotsFull()) {
                    // GE IS FULL. COLLECT ITEMS
                    GrandExchange.collectAll();
                    // Wait till it has collected and slots are empty
                    Waiting.waitUntil(() -> !MyExchange.isGrandExchangeSlotsFull());
                }
            }

            if (!successfullyPosted) {
                Log.error("Failed to post item after 5 attempts");
            }

            // Check if GE is full
            if (MyExchange.isGrandExchangeSlotsFull()) {
                // GE IS FULL. COLLECT ITEMS
                GrandExchange.collectAll();
                // Wait till it has collected and slots are empty
                Waiting.waitUntil(() -> !MyExchange.isGrandExchangeSlotsFull());
            }
        }
        Waiting.waitNormal(2000, 250);
        // Collect at the end
        if (MyExchange.hasOfferToCollect()){
            GrandExchange.collectAll();
        }

        if (shouldRepeat) {
            sellLoot();
        }
        MyExchange.closeExchange();
        MyBanker.openBank();
        MyBanker.depositInventory();
        shouldRepeat = false;

        mule();
    }

    public static void mule() {
        if (MuleManager.hasEnoughToMule()) {
            MuleManager.takeOutGp();
            try {
                if (!MyRevsClient.getScript().getSocketClient().getSocket().getInetAddress().isReachable(5000)) {
                    MyRevsClient.getScript().getSocketClient().startConnection("127.0.0.1", 6668);
                }

                var msg = MyRevsClient.getScript().getSocketClient().sendMessage(MyPlayer.getUsername());
                Waiting.wait(2000);
                Log.debug(msg);
                var content = msg.split(" ");
                for (String s : content) {
                    Log.debug(s);
                }
                int x = Integer.parseInt(content[0]);
                int y = Integer.parseInt(content[1]);
                int z = Integer.parseInt(content[2]);
                WorldTile mulingPosition = new WorldTile(x, y, z);
                if (!mulingPosition.isRendered() || !mulingPosition.isRendered()) {
                    GlobalWalking.walkTo(mulingPosition);
                }
                String mulerName = null;
                if (content.length == 6) {
                    mulerName = content[3] + " " + content[4];
                } else if (content.length == 7) {
                    mulerName = content[3] + " " + content[4] + " " + content[5];
                } else if (content.length == 8) {
                    mulerName = content[3] + " " + content[4] + " " + content[5] + " " + content[6];
                }

                int world = 0;

                if (content.length == 6) {
                    world = Integer.parseInt(content[5]);
                } else if (content.length == 7) {
                    world = Integer.parseInt(content[6]);
                } else if (content.length == 8) {
                    world = Integer.parseInt(content[7]);
                }

                if (WorldHopper.getCurrentWorld() != world) {
                    WorldHopper.hop(world);

                }
                inFirstTrade = false;
                trade(mulerName);

            } catch (Exception e) {
                MyRevsClient.getScript().getSocketClient().startConnection("localhost", 6668);

                Log.debug("Tried connecting to mule but couldn't");
                Log.error(e);
            }
        }
        WorldManager.hopToRandomMemberWorldWithRequirements();
    }

    private static void trade(String mulerName){
        while (Query.players().nameEquals(mulerName).findFirst().isEmpty()) {
            Waiting.wait(200);
        }

        if (Query.players().nameEquals(mulerName).findFirst().isPresent()) {
            Log.debug("Muler found! " + mulerName);
            Waiting.waitNormal(5000, 250);
            Log.debug("Trading muler..");
            Query.players().nameEquals(mulerName).findFirst().map(muler -> muler.interact("Trade with"));

            //Waiting.waitNormal(2000, 200);


            if (!inFirstTrade) {

                Log.debug("In first trade!");
                var success = Waiting.waitUntil(20000, () -> {
                    TradeScreen.getStage().filter(tra -> tra == TradeScreen.Stage.FIRST_WINDOW).map(tradeScreen -> {
                        Log.debug("In first trade");
                        inFirstTrade = true;

                            Log.debug("First trade");
                            TradeScreen.offerAll(995);
                            Waiting.wait(4000);
                            TradeScreen.accept();
                            return true;
                    });
                    return false;
                });

                if (!success && !inFirstTrade) {
                    trade(mulerName);
                    return;
                }
            }

            Waiting.waitNormal(3000, 300);

            Log.debug("2nd trade approached");
            // Second trade
            TradeScreen.getStage().filter(tra -> tra == TradeScreen.Stage.SECOND_WINDOW).map(tradeScreen -> {
                    Waiting.wait(2000);
                    TradeScreen.accept();
                    Waiting.wait(2000);
                    return true;
            });

            MuleManager.incrementAmountTimesMuled();
            MyScriptVariables.setTimesMuled(String.valueOf(MuleManager.getAmountTimesMuled()));
        }
    }

    public static void sellBow() {
            Log.debug("Selling bow");
        if (Query.bank().nameContains("Craw's bow").isAny()) {
            if (Skill.RANGED.getActualLevel() >= 75) {
                if (Bank.contains("Craw's bow") || Bank.contains("Craw's bow (u)")) {
                    Query.bank().nameContains("Craw's bow").findFirst().map(c -> MyBanker.withdraw(c.getName(), 1, false));
                    MyBanker.closeBank();
                    Query.inventory().nameEquals("Craw's bow").findFirst().ifPresent(bow -> {
                        Waiting.waitUntil(() -> bow.click("Uncharge"));
                        Waiting.waitNormal(1250, 125);
                        clickWidget("Yes", 584, 1);
                        Waiting.waitUntil(() -> Inventory.contains(22547) && Inventory.contains(21820));
                        MyBanker.openBank();
                    });
                }
            }
        }
        for (var item : Inventory.getAll()) {
            // Will wait until the offer shows up in the GE.
            if (item.getId() == 995) {
                continue;
            }
            boolean successfullyPosted = false;
            int attempts = 0;

            while (!successfullyPosted && attempts < 5) {
                if (!MyExchange.isExchangeOpen()) {
                    MyExchange.openExchange();
                }
                attempts++;
                successfullyPosted = MyExchange.createGrandExchangeSellOrder(item);
                // Check if GE is full
                if (MyExchange.isGrandExchangeSlotsFull()) {
                    // GE IS FULL. COLLECT ITEMS
                    GrandExchange.collectAll();
                    // Wait till it has collected and slots are empty
                    Waiting.waitUntil(() -> !MyExchange.isGrandExchangeSlotsFull());
                }
            }

            if (!successfullyPosted) {
                Log.error("Failed to post item after 5 attempts");
            }

            // Check if GE is full
            if (MyExchange.isGrandExchangeSlotsFull()) {
                // GE IS FULL. COLLECT ITEMS
                GrandExchange.collectAll();
                // Wait till it has collected and slots are empty
                Waiting.waitUntil(() -> !MyExchange.isGrandExchangeSlotsFull());
            }
        }
        Waiting.waitNormal(2000, 250);
        // Collect at the end
        if (MyExchange.hasOfferToCollect()){
            GrandExchange.collectAll();
        }

        MyExchange.closeExchange();
        MyBanker.openBank();
    }


    public static void restockFromBank(List<String> itemsTobuy) {
        MyExchange.closeExchange();
        openBank();
        Bank.withdrawAll("Coins");
        Waiting.wait(3500);
        Bank.close();
        Waiting.wait(3500);
        MyExchange.openExchange();

        for (var item : itemsTobuy) {
            if (item.contains("Blighted super restore") || item.contains("Saradomin brew") || item.contains("Divine ranging potion") || item.equals("Ring of recoil")) {
                MyExchange.createGrandExchangeBuyOrder(item, 100, 0, true);
            } else if (item.contains("Shark")) {
                MyExchange.createGrandExchangeBuyOrder(item, 300, 0, true);
            } else if (item.equals("Leather boots") || item.equals("Leather body") || item.equals("Coif") || item.contains("Snakeskin")) {
                    MyExchange.createGrandExchangeBuyOrder(item, 20, 3000, false);
            } else if (item.equals("Bandos cloak")) {
                MyExchange.createGrandExchangeBuyOrder(item, 1, 0, true);
            } else if (item.equals("Amulet of avarice")) {
                MyExchange.createGrandExchangeBuyOrder(item, 1, 1000000, false);
            } else if (item.equals("Rune arrow")) {
                MyExchange.createGrandExchangeBuyOrder(item, 5000, 0, true);
            } else if (item.equals("Bracelet of ethereum (uncharged)")) {
                MyExchange.createGrandExchangeBuyOrder(item, 5, 0, true);
            }else{
                MyExchange.createGrandExchangeBuyOrder(item, 10, 0, true);
            }

            Waiting.wait(3000);
        }

        GrandExchange.collectAll();
        GrandExchange.close();
        openBank();
    }

    public static void buy(String itemName) {
        GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(itemName).quantity(4000).priceAdjustment(2).type(GrandExchangeOffer.Type.BUY).build());
        Waiting.wait(3000);
        GrandExchange.collectAll();
    }

    public static void buyFromBank(int itemId, int amount) {
        Waiting.waitUntil(() -> Bank.withdrawAll("Coins"));
        Waiting.waitUntil(() -> Inventory.contains("Coins"));
        Bank.close();
        MyExchange.openExchange();
        GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemId(itemId).quantity(amount).priceAdjustment(4).type(GrandExchangeOffer.Type.BUY).build());
        Waiting.wait(3000);
        GrandExchange.collectAll();
        GrandExchange.close();
        openBank();
        Waiting.waitUntil(() -> Bank.depositAll(itemId));
        Waiting.waitNormal(2000, 200);

        Bank.depositAll("Coins");
    }
}
