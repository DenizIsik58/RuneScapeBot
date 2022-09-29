package scripts.rev;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.pricing.Pricing;
import org.tribot.script.sdk.query.Query;
import scripts.api.MyBanker;
import scripts.api.MyClient;
import scripts.api.MyExchange;

public class BondManager {

    public static boolean haveMoneyForBond(){
        // Open bank and check our stack
        if (!MyBanker.openBank()){
            MyBanker.openBank();
        }
        if (Inventory.contains("Coins")) {
            Log.debug("Trying to deposit coins");
            Bank.depositAll("Coins");
            Waiting.wait(3000);
        }


        return Bank.getCount("Coins") >= (Pricing.lookupPrice(13190).orElse(0) + 1000000);
    }

    public static boolean haveLowMembershipDays(){
        return MyPlayer.getMembershipDaysRemaining() <= 2;
    }

    public static boolean haveNoMembershipDays(){

        return MyPlayer.getMembershipDaysRemaining() == 0;
    }

    public static boolean buyBond(){
        var membershipDays = MyPlayer.getMembershipDaysRemaining();

        if (!MyBanker.openBank()) {
            MyBanker.openBank();
        }

        // Withdraw our money
        if (!MyBanker.withdraw("Coins", 10000000, false)){
            MyBanker.withdraw("Coins", 10000000, false);
        }

        //Close bank
        if (!MyBanker.closeBank()){
            MyBanker.closeBank();
        }

        // Open GE
        if (MyExchange.openExchange()) {
            MyExchange.openExchange();
        }

        // Buy bond
        MyExchange.createGrandExchangeBuyOrder("Old school bond", 1, Pricing.lookupPrice(13190).orElse(0) + 1000000, false);

        // Wait 3 seconds to make the buy come in
        Waiting.wait(3000);

        // Collect bond
        GrandExchange.collectAll();

        //Exit GE
        if (!MyExchange.closeExchange()){
            MyExchange.closeExchange();
        }

        Waiting.waitUntil(MyExchange::closeExchange);
        Waiting.wait(1000);

        Query.inventory().nameContains("bond").findClosestToMouse().map(bond -> {
            if (!bond.click("Redeem")){
                bond.click("Redeem");
            }
            Waiting.waitUntil(() -> MyClient.isWidgetVisible(66, 7, 14));
            MyClient.clickWidget("1 Bond", 66, 7);

            Waiting.wait(1000);

            Waiting.waitUntil(() -> MyClient.isWidgetVisible(66, 24));

            Waiting.wait(1000);

            MyClient.clickWidget("Confirm", 66, 24);
            return true;
        });

        Waiting.wait(2500);
        return MyPlayer.getMembershipDaysRemaining() > membershipDays;
    }
}
