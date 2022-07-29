package scripts;

import org.tribot.script.sdk.Bank;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.WorldHopper;
import org.tribot.script.sdk.query.Query;

public class MuleManager {


    public static boolean hopToMulerWorld(int world){
        return WorldHopper.hop(world);
    }

    public static int takeOutGp(){
        //Log.info("TAKING OUT GP");
        var gp = Query.bank().nameEquals("Coins").findFirst().orElse(null);
        int amount = 0;
        if (gp != null){
            amount = gp.getStack() - 2000000;
            Bank.withdraw("Coins", amount);
            Waiting.waitUntil(() -> Bank.contains("Coins"));
        }else {
            //Log.info("Couldnt find gold");
        }

        Bank.close();
        return amount;
    }
    public static boolean hasEnoughToMule(){
        return Query.bank().nameEquals("Coins").findFirst().get().getStack() >= 700000;
    }
}
