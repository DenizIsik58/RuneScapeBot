package scripts.api;

import org.tribot.script.sdk.Bank;
import org.tribot.script.sdk.GrandExchange;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Area;
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


    // this is good for adding in later other things like "if in house and has jewellery box use that wealth"
    public static boolean walkToGrandExchange() {
        if (isExchangeNearby()) return true;
        if (MyTeleporting.Wealth.GrandExchange.canUseTeleport()) {
            MyBanker.closeBank();
            return MyTeleporting.Wealth.GrandExchange.useTeleport();
        }
        if (Bank.isNearby()) {
            MyBanker.openBank();
            Query.bank().nameContains("Ring of wealth (").findFirst().ifPresent(wealth -> {
                MyBanker.withdraw(wealth.getId(), 1, false);
            });
        }
        GlobalWalking.walkTo(grandExchangeArea.getRandomTile());
        return Waiting.waitUntil(5000, MyExchange::isExchangeNearby);
    }

}
