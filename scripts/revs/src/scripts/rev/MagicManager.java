package scripts.rev;

import org.tribot.script.sdk.Combat;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.MyPlayer;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Projectile;
import scripts.api.MyScriptExtension;
import scripts.api.MyScriptVariables;

import java.util.Optional;

public class MagicManager implements Runnable {


    @Override
    public void run() {
        while (MyRevsClient.getScript().getPlayerDetectionThread() != null) {
            DetectPlayerThread.setProjectile();
            Waiting.wait(50);
        }
    }
}
