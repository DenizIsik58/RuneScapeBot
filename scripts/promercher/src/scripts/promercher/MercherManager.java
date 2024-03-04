package scripts.promercher;

import obf.Qe;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tribot.api2007.types.RSGEOffer;
import org.tribot.script.sdk.*;

import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.GrandExchangeOffer;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.types.definitions.ItemDefinition;
import scripts.api.MyExchange;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MercherManager {

    public static List<Integer> currentlyMerching = new ArrayList<>();
    public static Set<Integer> boughtItems = new HashSet<>();
    public static Set<Integer> soldItems = new HashSet<>();

    public static void addIDToList(int id) {
        currentlyMerching.add(id);
    }

    public static void removeIDFromList(int index){
        currentlyMerching.remove(index);
    }

    public static List<Integer> getCurrentlyMerching(){
        return currentlyMerching;
    }

    public static boolean contains(int itemId){
        return currentlyMerching.contains(itemId);
    }
    public static AtomicBoolean isMerchingSingleItem = new AtomicBoolean(false);


    public static void startMerching() {
        JSONArray jsonItems = GrandExchangeManager.sortAndGetHighestMarginItem();
        if (jsonItems == null) {
            // Handle the case where jsonItems is null
            Log.error("Failed to retrieve items from Grand Exchange. Exiting merching process.");
            return;
        }

        while (!MyExchange.isGrandExchangeSlotsFull()) {
            int myCoins = Inventory.getCount("Coins");

            JSONObject itemToMerch = GrandExchangeManager.findItemByEntryPrice(jsonItems, myCoins);
            if (itemToMerch == null) {
                // No more items to merch based on available coins
                Log.info("No more profitable items found based on available coins. Exiting merching process.");
                break;
            }

            int itemID = itemToMerch.getInt("id");
            int priceOfItem = itemToMerch.getInt("price");
            int limit = itemToMerch.getInt("limit");
            int quantity = calculateAmountOfItemsToBuy(myCoins, priceOfItem, limit);
            String itemName = ItemDefinition.get(itemID).get().getName();

            Log.info("FOUND : " + itemName + " " + itemToMerch);
            Log.info("EXPECTED PROFIT: " + quantity * itemToMerch.getInt("margin"));

            Waiting.waitUntil(4500, () -> MyExchange.createGrandExchangeBuyOrder(itemName, quantity, priceOfItem, false));
            Waiting.wait(2342);
            if (Query.grandExchangeOffers().itemIdEquals(itemID).isAny()){
                // Ensure that we really bought it!
                Log.info("Made a buy offer for: " + itemName + " ID: " + itemID + " added to list");
                addIDToList(itemID);
                boughtItems.add(itemID);
                Log.debug(List.of(currentlyMerching));
            }
            Waiting.wait(3420);
        }
    }


    public static void startSelling(){
        while (!currentlyMerching.isEmpty() && !MyExchange.isGrandExchangeSlotsFull()) {
            Log.debug(List.of(currentlyMerching));


            if (MyExchange.hasOfferToCollect()){
                Query.grandExchangeOffers().statusEquals(GrandExchangeOffer.Status.COMPLETED).typeEquals(GrandExchangeOffer.Type.BUY).findFirst().ifPresent(bought -> {
                    Waiting.wait(1000);
                    MyExchange.collectGrandExhcangeOffers();
                    Log.debug("BUY: " + bought.getItemId());
                    if (!contains(bought.getItemId())){
                        boughtItems.add(bought.getItemId());
                        addIDToList(currentlyMerching.indexOf(bought.getItemId()));
                    }

                });

                Query.grandExchangeOffers().statusEquals(GrandExchangeOffer.Status.COMPLETED).typeEquals(GrandExchangeOffer.Type.SELL).findFirst().ifPresent(sold -> {
                    Waiting.wait(1000);
                    MyExchange.collectGrandExhcangeOffers();
                    Log.debug("SOLD: " + sold.getItemId());
                    removeIDFromList(currentlyMerching.indexOf(sold.getItemId()));
                    boughtItems.remove(sold.getItemId());
                    Log.debug(List.of(currentlyMerching));
                });


                Waiting.wait(2304);
            }

            if (Inventory.getAll().size() <= 1) {
                Log.debug("No items to sell in inventory");
                break;
            }

            for (var invItem : Inventory.getAll()){
                if (invItem.getName().equals("Coins")) {
                    continue;
                }
                int itemId = 0;

                for (var id: boughtItems) {
                    itemId = Query.inventory().idEquals(id).isNoted().isAny() ? invItem.getId() + 1 : invItem.getId();
                }


                if (invItem.getId() == itemId) {
                    Log.debug("id:" + itemId);

                    if (!MyExchange.isGrandExchangeSlotsFull()) {
                        JSONArray jsonItems = GrandExchangeManager.sortAndGetHighestMarginItem();
                        assert jsonItems != null;

                        Log.info(itemId);
                        int finalItemId = itemId;
                        JSONObject itemToSell = jsonItems.toList().stream()
                                .filter(obj -> {
                                    if (obj instanceof JSONObject) {
                                        return ((JSONObject) obj).getInt("id") == (Query.inventory().nameEquals(invItem.getName()).isNoted().isAny() ? finalItemId - 1 : finalItemId);
                                    } else if (obj instanceof HashMap) {
                                        // Assuming the key "id" exists in the HashMap
                                        Object id = ((HashMap<?, ?>) obj).get("id");
                                        return id instanceof Integer && ((Integer) id).intValue() == (Query.inventory().nameEquals(invItem.getName()).isNoted().isAny() ? finalItemId - 1 : finalItemId);
                                    }
                                    return false;
                                })
                                .map(obj -> {
                                    if (obj instanceof JSONObject) {
                                        return (JSONObject) obj;
                                    } else if (obj instanceof HashMap) {
                                        // Convert HashMap to JSONObject if necessary
                                        return new JSONObject((Map<?, ?>) obj);
                                    }
                                    return null;
                                })
                                .findFirst()
                                .orElse(null);

                        if (itemToSell == null) {
                            // try again
                            Log.info("Item to sell not found. Skipping...");
                            continue;
                        }

                        int margin = itemToSell.getInt("margin");
                        int itemBuyPrice = itemToSell.getInt("price");
                        int sellPrice = margin + itemBuyPrice;
                        for (var item: Inventory.getAll()) {
                            if (item.getId() == itemId) {
                                MyExchange.createGrandExchangeSellOrder(item, sellPrice);
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean isMerchingSingleItem(){
        return isMerchingSingleItem.get();
    }

    public static void getIsMerchingSingleItem(boolean value) {
        isMerchingSingleItem.set(value);
    }

    public static int calculateAmountOfItemsToBuy(int amountOfCoins, int pricePerItem, int limit){
        if (MyPlayer.isMember()) {
            // We have 8 slots
            return 0;
        }
        return Math.min((amountOfCoins / pricePerItem), limit);
    }
}
