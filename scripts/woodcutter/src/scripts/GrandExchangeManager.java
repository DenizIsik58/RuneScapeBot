package scripts;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.types.GrandExchangeOffer;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;

import java.util.List;

public class GrandExchangeManager {

    public static void sellLogsIfPossible(List<String> logs, WorldTile GE, String currentAxe){

        var amountOfLogs = logs.stream().mapToInt(Bank::getCount).sum();
        Log.info(amountOfLogs);
        if (amountOfLogs >= 400) {
            BankSettings.setNoteEnabled(true);
            logs.forEach(Bank::withdrawAll);
            BankSettings.setNoteEnabled(false);
            Bank.close();

            if (!GrandExchange.isNearby()) {
                GlobalWalking.walkTo(GE);
            }
            if (!GrandExchange.isOpen()) {
                GrandExchange.open();
                var inventory = Inventory.getAll();
                for (var item :
                        inventory) {
                    if (item.getName().contains("logs")) {
                        logs.forEach(log -> GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(log).quantity(Inventory.getCount(item.getName())).priceAdjustment(-5).type(GrandExchangeOffer.Type.SELL).build()));
                    }
                }
                GrandExchange.collectAll();
                GrandExchange.close();
                if (!Bank.contains(currentAxe) && !Inventory.contains(currentAxe) && !MyPlayer.get().flatMap(player -> player.getEquippedItem(Equipment.Slot.WEAPON)).get().getName().equals(currentAxe)){
                    buyPrefferedAxe(currentAxe);
                    GrandExchange.collectAll();
                    GrandExchange.close();
                }
            }
        }
    }

    public static void buyPrefferedAxe(String currentAxe){
        GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().searchText(currentAxe).type(GrandExchangeOffer.Type.BUY).priceAdjustment(3).build());
    }
}
