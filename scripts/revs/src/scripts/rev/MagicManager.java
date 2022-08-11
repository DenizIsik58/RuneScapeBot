package scripts.rev;

import obf.De;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Projectile;
import scripts.api.MyScriptExtension;
import scripts.api.MyScriptVariables;

import java.util.Optional;

public class MagicManager implements Runnable {


    @Override
    public void run() {
        while (MyRevsClient.getScript().getPlayerDetectionThread() != null) {
            var pker = DetectPlayerThread.getPker();
            DetectPlayerThread.setProjectile();
            if (DetectPlayerThread.isFrozen()) {
                // Else
                // Do antipk here
                PrayerManager.enablePrayer(Prayer.PROTECT_ITEMS);

                // 2. Fight back pker if not
                if (pker != null) {
                    if (Query.players().nameEquals(pker.getName()).isMyPlayerNotInteractingWith().isAny()) {
                        Log.debug("Not fighting pker");
                        pker.click();
                    }

                    DetectPlayerThread.handleEatAndPrayer(pker);

                    // 3. try to run away if we are not frozen
                    // TODO: Currently only runs no matter what. We need to figure out how we are frozen.
            /*if (MyRevsClient.myPlayerIsInCave()) {
                ensureWalkingPermission();

            } else {
                handleEatAndPrayer(pker);
                ensureWalkingPermission();
                MyExchange.walkToGrandExchange();
            }*/

                    Log.debug("We are frozen!");
                }
                }

            Waiting.wait(50);
        }
    }
}
