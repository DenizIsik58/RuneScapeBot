package scripts.api;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Area;
import org.tribot.script.sdk.types.GrandExchangeOffer;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;

public class MyExchange {

    private static final Area grandExchangeArea = Area.fromPolygon(new WorldTile(3140, 3508, 0),
            new WorldTile(3146, 3514, 0),
            new WorldTile(3188, 3515, 0),
            new WorldTile(3196, 3507, 0),
            new WorldTile(3188, 3497, 0),
            new WorldTile(3189, 3481, 0),
            new WorldTile(3181, 3470, 0),
            new WorldTile(3165, 3469, 0),
            new WorldTile(3144, 3469, 0),
            new WorldTile(3141, 3477, 0));

    public static boolean isExchangeOpen() { return GrandExchange.isOpen(); }
    public static boolean isExchangeClosed() { return !GrandExchange.isOpen(); }

    public static boolean isExchangeNearby() { return grandExchangeArea.containsMyPlayer(); }
    public static boolean isExchangeNotNearby() { return !isExchangeNearby(); }

    public static boolean closeExchange() {
        if (isExchangeClosed()) return true;
        return GrandExchange.close() && Waiting.waitUntil(2000, MyExchange::isExchangeClosed);
    }

    public static boolean openExchange() {
        if (isExchangeOpen()) return true;
        MyBanker.closeBank();
        if (isExchangeNotNearby()) {
            if (!walkToGrandExchange()) {
                Log.warn("Could not open Grand Exchange because walking to it failed.");
                return false;
            }
        }
        GrandExchange.open();
        return Waiting.waitUntil(3000, MyExchange::isExchangeOpen);
    }

    public static boolean hasOfferToCollect(){
        return Query.grandExchangeOffers().statusEquals(GrandExchangeOffer.Status.COMPLETED).isAny();
    }

    public static boolean isGrandExchangeSlotsFull(){
        return !Query.grandExchangeOffers().statusEquals(GrandExchangeOffer.Status.EMPTY).isAny();
    }

    public static boolean collectGrandExhcangeOffers() {
        return Waiting.waitUntil(2000, GrandExchange::collectAll);
    }

    public static boolean createGrandExchangeSellOrder(InventoryItem item){
        GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(item.getName()).quantity(Inventory.getCount(item.getId())).priceAdjustment(-3).type(GrandExchangeOffer.Type.SELL).build());
        return Waiting.waitUntil(2000, () -> Query.grandExchangeOffers().itemNameEquals(item.getName()).isAny());
    }

    public static boolean createGrandExchangeSellOrder(InventoryItem item, int price){
        GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(item.getName()).quantity(Inventory.getCount(item.getId())).price(price).type(GrandExchangeOffer.Type.SELL).build());
        return Waiting.waitUntil(2000, () -> Query.grandExchangeOffers().itemNameEquals(item.getName()).isAny());
    }

    public static boolean createGrandExchangeBuyOrder(String item, int quantity,  int price, boolean adjustment){
        openExchange();
        if (!Query.grandExchangeOffers().itemNameEquals(item).isAny()) {
            if (adjustment) {
                GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(item).quantity(quantity).priceAdjustment(4).type(GrandExchangeOffer.Type.BUY).build());
            } else {
                GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(item).quantity(quantity).price(price).type(GrandExchangeOffer.Type.BUY).build());
            }
        }
        return Waiting.waitUntil(2000, () -> Query.grandExchangeOffers().itemNameEquals(item).isAny());
    }


    // this is good for adding in later other things like "if in house and has jewellery box use that wealth"
    public static boolean walkToGrandExchange() {
        if (isExchangeNearby()) return true;

        //var lastTB = MyScriptVariables.getVariable("lastTeleblockNotification", 0L);

        Log.debug("Trying to walk towards grand exchange");
        if (MyTeleporting.Wealth.GrandExchange.canUseTeleport()) {
            MyBanker.closeBank();
            MyTeleporting.Wealth.GrandExchange.useTeleport();
            return Waiting.waitUntil(5000, MyExchange::isExchangeNearby);
        }

        if (Bank.isNearby() && !Query.inventory().nameContains("Ring of wealth (").isAny() || !Query.equipment().nameContains("Ring of wealth (").isAny()) {
            MyBanker.openBank();
            Query.bank().nameContains("Ring of wealth (").findFirst().ifPresent(wealth -> {
                MyBanker.withdraw(wealth.getId(), 1, false);
            });
            MyBanker.closeBank();
        }
        GlobalWalking.walkTo(grandExchangeArea.getRandomTile());
        return Waiting.waitUntil(5000, MyExchange::isExchangeNearby);
    }

    public static Area getGrandExchangeArea() {
        return grandExchangeArea;
    }
}
