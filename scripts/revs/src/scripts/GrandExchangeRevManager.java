package scripts;

import net.sourceforge.jdistlib.InvNormal;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.GrandExchangeOffer;
import org.tribot.script.sdk.types.World;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.types.definitions.ItemDefinition;
import org.tribot.script.sdk.walking.GlobalWalking;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static scripts.BankManagerRevenant.openBank;

public class GrandExchangeRevManager {

    private static boolean shouldRepeat = false;

    public static void openGE(){
        if (!GrandExchange.isOpen()){
            GrandExchange.open();
        }
    }
    public static void sellLoot() {
        shouldRepeat = false;
        openBank();
        Bank.depositInventory();
        if (!BankSettings.isNoteEnabled()){
            BankSettings.setNoteEnabled(true);
        }
        Bank.withdrawAll("Coins");

        for (var item : LootingManager.getLootToPickUp()) {
            if (item.equals("Looting bag") || item.equals("Coins")){
                continue;
            }
            if (item.contains("Bracelet of eth")){
                if (Bank.getCount(item) < 10) {
                    continue;
                }
                Bank.withdraw(item, Bank.getCount(item) - Bank.getCount(item) - 10);
            }
            if (Inventory.isFull()){
                shouldRepeat = true;
                break;
            }
            if (Query.bank().nameEquals(item).isAny()) {
                Waiting.waitUntil(() -> Bank.withdrawAll(item));
            }else {
                RevenantScript.state = State.BANKING;
            }
        }

        BankSettings.setNoteEnabled(false);
        Waiting.waitUntil(Bank::close);
        Waiting.wait(2000);
        openGE();
        Waiting.wait(2000);


        while(true){
            int counter = 0;
            if (Inventory.getAll().size() == 1 && Inventory.contains("Coins")){
                break;
            }

            while(counter != 8){
                if (Inventory.getAll().size() == 1 && Query.inventory().nameEquals("Coins").isAny()){
                    break;
                }
                for (var item : Inventory.getAll()){
                    if (counter == 8){
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
        BankManagerRevenant.openBank();
        Bank.depositAll("Coins");
        Waiting.wait(5000);

        var gp = Query.bank().nameEquals("Coins").findFirst().orElse(null);
        assert gp != null;
        if (MuleManager.hasEnoughToMule()){

            try {
                if (!MulingClient.getClientSocket().getInetAddress().isReachable(5000)){
                }

                var msg = RevenantScript.getSocketClient().sendMessage("I want to mule! " + MyPlayer.get().get().getName());
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
                //Log.info(e);
            }

        }
        if (shouldRepeat){
            sellLoot();
        }else {
            RevenantScript.state = State.BANKING;
        }
    }




    public static void restockFromBank(List<String> itemsTobuy, List<String> stackables){

        Bank.withdrawAll("Coins");
        Waiting.wait(3500);
        Bank.close();
        Waiting.wait(3500);
        openGE();

        for (var item : itemsTobuy) {
            if (item.contains("Prayer pot") || item.contains("Shark") || item.contains("Divine ranging potion")) {
                GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(item).quantity(100).priceAdjustment(2).type(GrandExchangeOffer.Type.BUY).build());
            }else {
                GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(item).quantity(10).priceAdjustment(2).type(GrandExchangeOffer.Type.BUY).build());
            }
            Waiting.wait(3000);
        }
        if (stackables.contains("Revenant ether")) {
            GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName("Revenant ether").quantity(4000).priceAdjustment(2).type(GrandExchangeOffer.Type.BUY).build());
            Waiting.wait(3000);
        }

        GrandExchange.collectAll();
        GrandExchange.close();
        BankManagerRevenant.withdrawGear();
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
