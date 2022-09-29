package scripts.rev;

import dax.walker.models.WaitCondition;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.interfaces.Stackable;
import org.tribot.script.sdk.query.Query;
import scripts.api.MyBanker;

public class MuleManager {

    private static int amountTimesMuled = 0;

    public static boolean hopToMulerWorld(int world){
        return WorldHopper.hop(world);
    }


    public static void takeOutGp(){
        MyBanker.depositAll();
        //Query.bank().nameEquals("Coins").findFirst().ifPresent(gp -> MyBanker.withdraw(995, gp.getStack() - 3000000, false));
        MyBanker.withdraw("Coins", Bank.getCount("Coins") - 3000000, false);
        Waiting.waitNormal(1500, 100);
        if (!Inventory.contains("Coins")) {
            Log.debug("We don't have cash in invy. Trying again");
            MyBanker.withdraw("Coins", Bank.getCount("Coins") - 3000000, false);
        }
        if (!MyBanker.closeBank()){
            MyBanker.closeBank();
        }
    }
    public static boolean hasEnoughToMule(){
        var stack = Bank.getCount("Coins");
        return stack >= 10000000;
    }

    public static int getAmountTimesMuled() {
        return amountTimesMuled;
    }

    public static void incrementAmountTimesMuled() {
        MuleManager.amountTimesMuled++;
    }
}
