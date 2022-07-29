package scripts;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.input.Mouse;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Player;
import org.tribot.script.sdk.walking.WalkState;

public class PkerDetecter implements Runnable {


    public static void quickTele() {
        Mouse.setSpeed(2000);
        if (MyRevsClient.myPlayerIsInGE()) {
            RevenantScript.state = State.BANKING;
        }
        //Log.info("Quick teleporting");
        Equipment.Slot.RING.getItem().map(c -> c.click("Grand Exchange"));
        Waiting.waitUntil(MyRevsClient::myPlayerIsInGE);
        Mouse.setSpeed(200);
        RevenantScript.state = State.BANKING;
    }

    public static boolean isPkerDetected() {
        var interactables = Query.players().withinCombatLevels(Combat.getWildernessLevel());
        if (interactables.count() != 0) {
            if (Query.players().withinCombatLevels(Combat.getWildernessLevel()).hasSkullIcon().isAny()) {
                //Log.info("PKer detected");
                return true;
            }

            if (Query.players().withinCombatLevels(Combat.getWildernessLevel()).isAny()) {
                if (Query.players().isEquipped(22550).isAny() || Query.players().isEquipped(12926).isAny() || Query.players().isEquipped("Viggora's chainmace").isAny()) {
                    //Log.info("Pvmer detected");
                    return false;
                } else {
                    //Log.info("Possible PKer detected");
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void run() {
        //Log.info("NEW THREAD HAS BEEN STARTED");
        while(true){
            while (RevenantScript.state == State.WALKING || RevenantScript.state == State.KILLING) {
                //Log.info("I'm CURRENTLY RUNNING!");
                if (PkerDetecter.isPkerDetected()) {
                    //Log.info("PKER DETECTED!!");
                    quickTele();
                }
                Waiting.wait(50);
            }
            Waiting.wait(50);
        }
    }
}

