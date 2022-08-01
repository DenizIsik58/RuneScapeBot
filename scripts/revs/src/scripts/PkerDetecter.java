package scripts;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.input.Mouse;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Player;
import org.tribot.script.sdk.walking.WalkState;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

public class PkerDetecter implements Runnable {

    private BooleanSupplier running;

    public PkerDetecter(BooleanSupplier running) {
            this.running = running;
    }

    public static void quickTele() {
        Log.info("Quick teleporting");
        if (Equipment.Slot.RING.getItem().map(c -> c.click("Grand Exchange")).orElse(false)) {
            Waiting.waitUntil(6000, MyRevsClient::myPlayerIsInGE);
        } else Log.warn("Failed teleporting to Grand Exchange");

    }

    public static boolean isPkerDetected() {
        var interactables = Query.players().withinCombatLevels(Combat.getWildernessLevel());
        if (interactables.count() != 0) {
            if (Query.players().withinCombatLevels(Combat.getWildernessLevel()).hasSkullIcon().isAny()) {
                Log.info("PKer detected");
                return true;
            }

            if (Query.players().withinCombatLevels(Combat.getWildernessLevel()).isAny()) {
                if (Query.players().isEquipped(22550).isAny() || Query.players().isEquipped(12926).isAny() || Query.players().isEquipped("Viggora's chainmace").isAny() || Query.players().isEquipped("Magic shortbow").isAny() || Query.players().isEquipped("Magic shortbow (i)").isAny()) {
                    Log.info("Pvmer detected");
                    return false;
                } else {
                    Log.info("Possible PKer detected");
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void run() {

        while(running.getAsBoolean()){
            if (RevenantScript.isState(State.WALKING) || RevenantScript.isState(State.KILLING) || RevenantScript.isState(State.LOOTING)) {

                if (PkerDetecter.isPkerDetected()) {
                    quickTele();
                }
            }
            Waiting.wait(50);
        }
    }
}

