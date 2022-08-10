package scripts.rev;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.GrandExchangeOffer;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.util.Retry;
import org.tribot.script.sdk.walking.GlobalWalking;
import scripts.api.MyBanker;
import scripts.api.MyExchange;
import scripts.api.MyScriptVariables;

import java.util.List;

import static scripts.api.MyBanker.closeBank;
import static scripts.api.MyBanker.openBank;
import static scripts.api.utility.Utility.distinctBy;

public class GrandExchangeRevManager {

    private static boolean shouldRepeat = false;

    public static List<InventoryItem> getAllSellItems() {
        return Query.inventory().filter(distinctBy(InventoryItem::getIndex)).filter(item -> item.getId() != 995 && item.getId() != 22547).distinctById().toList();
    }


    public static void sellLoot() {
        Log.debug("Trying to sell loot");
        closeBank();
        MyExchange.walkToGrandExchange();
        openBank();
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
        var itemsToSell = 0;
        for (var item : LootingManager.getLootToPickUp()) {
            if (item.equals("Looting bag") || item.equals("Coins") || item.equals("Craw's bow (u)")) {
                continue;
            }

            if (Inventory.isFull()) {
                shouldRepeat = true;
                break;
            }

            if (item.equals("Bracelet of ethereum (uncharged)")) {
                if (!Bank.contains(item)) {
                    break;
                }
                if (Bank.getCount(item) <= 5 && Bank.contains(item)) {
                    continue;
                }
                Log.warn("Pulling out bracelet");
                MyBanker.withdraw(item, Bank.getCount(item) - 5, true);
                Waiting.waitUntil(() -> Inventory.contains(item));
                itemsToSell++;
                continue;
            }

            if (Query.bank().nameEquals(item).isAny()) {
                itemsToSell++;
                MyBanker.withdraw(item, 10000000, true);
            }
        }
        if (itemsToSell == 0) {
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
                successfullyPosted = MyExchange.createGrandExchangeOffer(item);
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

                var msg = MyRevsClient.getScript().getSocketClient().sendMessage("I want to mule! " + MyPlayer.get().get().getName());
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
            String finalMulerName = mulerName;

            TradeScreen.getStage().ifPresent(screen -> {
                var inTrade = Waiting.waitUntil(100000, () -> screen == TradeScreen.Stage.FIRST_WINDOW);
                if (!inTrade){
                    Log.debug("Not in trade. Trying again..");
                    retryTrade(finalMulerName);
                }
            });
            //Waiting.waitNormal(2000, 200);

            var inTrade = Waiting.waitUntil(25000, () -> {
                TradeScreen.getStage().map(tradeScreen -> {
                    if (tradeScreen == TradeScreen.Stage.FIRST_WINDOW) {
                        TradeScreen.offerAll(995);
                        Waiting.wait(4000);
                        TradeScreen.accept();
                        return true;
                    }
                    return false;
                });
                return false;
            });

            if (!inTrade) {
                Log.debug("Couldn't get in trade. Trying again..");
                trade(mulerName);
            }

            Waiting.waitNormal(3000, 300);


            // Second trade
            TradeScreen.getStage().map(tradeScreen -> {
                if (tradeScreen == TradeScreen.Stage.SECOND_WINDOW) {
                    Waiting.wait(4000);
                    TradeScreen.accept();
                    return true;
                }
                return false;
            });
            MuleManager.incrementAmountTimesMuled();
            MyScriptVariables.setTimesMuled(String.valueOf(MuleManager.getAmountTimesMuled()));
        }
    }

    private static void retryTrade(String mulerName) {

            Query.players().nameEquals(mulerName).findFirst().map(muler -> muler.interact("Trade with"));
            TradeScreen.OtherPlayer.getName().ifPresent(muler -> {
                Waiting.waitUntil(()-> muler.equals(mulerName));
            });
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
            if (item.contains("Prayer pot") || item.contains("Shark") || item.contains("Divine ranging potion")) {
                GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(item).quantity(100).priceAdjustment(2).type(GrandExchangeOffer.Type.BUY).build());
            } else {
                if (item.equals("Leather boots") || item.equals("Leather body") || item.equals("Coif")) {
                    GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(item).quantity(10).price(1000).type(GrandExchangeOffer.Type.BUY).build());
                } else {
                    GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(item).quantity(10).priceAdjustment(5).type(GrandExchangeOffer.Type.BUY).build());
                }
            }
            Waiting.wait(3000);
        }
        if (itemsTobuy.contains("Revenant ether")) {
            GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName("Revenant ether").quantity(4000).priceAdjustment(2).type(GrandExchangeOffer.Type.BUY).build());
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
