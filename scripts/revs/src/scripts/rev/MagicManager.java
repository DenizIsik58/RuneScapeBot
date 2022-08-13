package scripts.rev;


import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;

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

                if (Combat.canUseSpecialAttack()) {
                    if (MyPlayer.getCurrentHealthPercent() < 70) {
                        Combat.activateSpecialAttack();
                    }
                }

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
