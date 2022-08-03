package scripts.rev;

import org.tribot.script.sdk.WorldHopper;
import org.tribot.script.sdk.interfaces.Stackable;
import org.tribot.script.sdk.query.Query;
import scripts.api.MyBanker;

public class MuleManager {


    public static boolean hopToMulerWorld(int world){
        return WorldHopper.hop(world);
    }

    public static void takeOutGp(){

        Query.bank().nameEquals("Coins").findFirst().ifPresent(gp -> MyBanker.withdraw(995, gp.getStack() - 2000000, false));
        MyBanker.closeBank();
    }
    public static boolean hasEnoughToMule(){
        var stack = Query.bank().nameEquals("Coins").findFirst().map(Stackable::getStack).orElse(0);
        return stack >= 7000000;
    }
}
