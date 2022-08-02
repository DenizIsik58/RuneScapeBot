package scripts.rev;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.interfaces.Stackable;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.GrandExchangeOffer;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import scripts.api.MyExchange;

import java.util.List;

import static scripts.api.MyBanker.openBank;

public class GrandExchangeRevManager {

    private static boolean shouldRepeat = false;

    public static void openGE(){
        if (!GrandExchange.isOpen()){
            GrandExchange.open();
        }
    }
    public static void sellLoot() {
        shouldRepeat = false;
        MyExchange.walkToGrandExchange();
        openBank();
        Bank.depositInventory();
        if (!BankSettings.isNoteEnabled()){
            BankSettings.setNoteEnabled(true);
        }
        Bank.withdrawAll("Coins");

        for (var item : LootingManager.getLootToPickUp()) {
            if (item.equals("Looting bag") || item.equals("Coins") || item.equals("Craw's bow (u)")){
                continue;
            }
            if (item.equals("Bracelet of ethereum (uncharged)")){
                if (Bank.getCount(item) <= 10) {
                    continue;
                }
                Log.warn("Pulling out bracelet");
                var stack =  Query.bank().nameEquals(item).findFirst().map(Stackable::getStack).orElse(0) - 10;
                Waiting.waitUntil(2000, () -> Bank.withdraw(item, stack));

                Log.warn("Done pulling out");
                if (Inventory.getCount(item) == stack){
                    Bank.deposit(item, 10);
                }
            }
            if (Inventory.isFull()){
                shouldRepeat = true;
                Log.warn("I BROKE OUT");
                break;
            }
            if (Query.bank().nameEquals(item).isAny()) {
                Waiting.waitUntil(() -> Bank.withdrawAll(item));
            }
        }
        Log.warn("IM HERE");

        //BankSettings.setNoteEnabled(false);
        Waiting.waitUntil(Bank::close);
        Waiting.waitUntil(GrandExchange::open);
        Waiting.wait(2000);


        while(true){
            Log.debug("IM IN WHILE LOOP");
            int counter = 0;
            if (Inventory.getAll().size() == 1 && Inventory.contains("Coins")){
                break;
            }

            while(counter != 8){
                if (Inventory.getAll().size() == 1 && Query.inventory().nameEquals("Coins").isAny()){
                    break;
                }
                for (var item : Inventory.getAll()){
                    if (counter == 8 || item.getName().equals("Craw's bow (u)")){
                        break;
                    }
                    GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(item.getName()).quantity(Inventory.getCount(item.getId())).priceAdjustment(-2).type(GrandExchangeOffer.Type.SELL).build());
                    counter++;
                }

                Waiting.wait(500);
            }
            Waiting.wait(2000);
            GrandExchange.collectAll();
        }

        GrandExchange.close();
        openBank();

        Bank.depositAll("Coins");
        Waiting.wait(5000);

        if (MuleManager.hasEnoughToMule()){
            MuleManager.takeOutGp();
            try {
                if (!MyRevsClient.getScript().getSocketClient().getSocket().getInetAddress().isReachable(5000)){
                    MyRevsClient.getScript().getSocketClient().startConnection( "127.0.0.1", 6668);
                }

                var msg = MyRevsClient.getScript().getSocketClient().sendMessage("I want to mule! " + MyPlayer.get().get().getName());
                Waiting.wait(2000);
                var content = msg.split(" ");
                int x = Integer.parseInt(content[0]);
                int y = Integer.parseInt(content[1]);
                int z = Integer.parseInt(content[2]);
                WorldTile mulingPosition = new WorldTile(x, y ,z);
                if (!mulingPosition.isRendered() || !mulingPosition.isRendered()){
                    GlobalWalking.walkTo(mulingPosition);
                }
                String mulerName = null;
                if (content.length == 6){
                    mulerName = content[3] + " " + content[4];
                }else if (content.length == 7) {
                    mulerName = content[3] + " " + content[4] + " " + content[5];
                }else if (content.length == 8) {
                    mulerName = content[3] + " " + content[4] + " " + content[5] + " " + content[6];
                }
                
                int world = 0;

                if (content.length == 6){
                    world = Integer.parseInt(content[5]);
                }else if (content.length == 7) {
                    world = Integer.parseInt(content[6]);
                }else if (content.length == 8) {
                    world = Integer.parseInt(content[7]);
                }

                if (WorldHopper.getCurrentWorld() != world){
                    int finalWorld = world;
                    Waiting.waitUntil(() -> MuleManager.hopToMulerWorld(finalWorld));
                }
                    Waiting.wait(10000);
                    var player = Query.players().nameEquals(mulerName).findFirst().orElse(null);
                    if (player != null){
                        player.interact("Trade with");
                        Waiting.wait(5000);
                    }
                    TradeScreen.offerAll(995);

                    TradeScreen.accept();
                    Waiting.wait(10000);
                    TradeScreen.accept();
                    Waiting.wait(10000);


            }catch (Exception e){

            }
        }


    }




    public static void restockFromBank(List<String> itemsTobuy){

        Bank.withdrawAll("Coins");
        Waiting.wait(3500);
        Bank.close();
        Waiting.wait(3500);
        openGE();

        for (var item : itemsTobuy) {
            if (item.contains("Prayer pot") || item.contains("Shark") || item.contains("Divine ranging potion")) {
                GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(item).quantity(100).priceAdjustment(2).type(GrandExchangeOffer.Type.BUY).build());
            }else {
                if (item.equals("Leather boots") || item.equals("Leather body") || item.equals("Coif")){
                    GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(item).quantity(10).price(1000).type(GrandExchangeOffer.Type.BUY).build());
                }else {
                    GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(item).quantity(10).priceAdjustment(2).type(GrandExchangeOffer.Type.BUY).build());
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

    public static void buy(String itemName){
        GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(itemName).quantity(4000).priceAdjustment(2).type(GrandExchangeOffer.Type.BUY).build());
        Waiting.wait(3000);
        GrandExchange.collectAll();
    }

    public static void buyFromBank(int itemId, int amount){
        Bank.withdrawAll("Coins");
        Bank.close();
        openGE();
        GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemId(itemId).quantity(amount).priceAdjustment(4).type(GrandExchangeOffer.Type.BUY).build());
        Waiting.wait(3000);
        GrandExchange.collectAll();
        GrandExchange.close();
        openBank();
        Bank.depositAll(itemId);
        Bank.depositAll("Coins");
    }
}
