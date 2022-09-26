package scripts.rev;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.input.Mouse;
import org.tribot.script.sdk.interfaces.PreBreakStartListener;

public class ScriptClass {

    private final PreBreakStartListener preBreakStartListener = breakLengthInMs -> {
        if (MyRevsClient.getScript().getPlayerDetectionThread().hasPkerBeenDetected()) {
            Log.debug("I'm in danger. Waiting for pk thread to finish");
            Waiting.waitUntil(1000 * 60 * 6, () -> !Combat.isInWilderness());
        }

        if (RevkillerManager.getTarget().isValid() || RevkillerManager.getTarget().getHealthBarPercent() != 0) {
            Log.debug("Killing target mob before breaking");
            if (RevkillerManager.getTarget() != null) {
                Waiting.waitUntil(40000, () -> RevkillerManager.getTarget().getHealthBarPercent() == 0 || !RevkillerManager.getTarget().isValid());
            }
            Waiting.wait(4000);
            LootingManager.loot();
        }


        if (Combat.isInWilderness()){
            TeleportManager.teleportOut();
            MyRevsClient.getScript().setState(State.BANKING);
            Waiting.wait(Math.toIntExact(breakLengthInMs));
        }
    };

    private final Runnable breakEndingListener = () -> {

    };

    public ScriptClass() {
        ScriptListening.addPreBreakStartListener(preBreakStartListener);
        ScriptListening.addBreakEndListener(breakEndingListener);
    }


}
