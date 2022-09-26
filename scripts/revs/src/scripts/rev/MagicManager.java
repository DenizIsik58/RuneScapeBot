package scripts.rev;


import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;

public class MagicManager implements Runnable {


    @Override
    public void run() {
        while (MyRevsClient.getScript().getPlayerDetectionThread() != null) {
            var pker = DetectPlayerThread.getPker();

            if (Combat.getCurrentAttackStyle() != Combat.AttackStyle.RAPID) {
                Combat.setAttackStyle(Combat.AttackStyle.RAPID);
            }

            DetectPlayerThread.setEntangle();

            if (DetectPlayerThread.isFrozen()) {
                // Else
                // Do antipk here
                PrayerManager.enablePrayer(Prayer.PROTECT_ITEMS);
                PrayerManager.enablePrayer(Prayer.EAGLE_EYE);
                if (Combat.canUseSpecialAttack()) {
                    if (!Combat.isSpecialAttackEnabled()) {
                        Combat.activateSpecialAttack();
                        Waiting.waitUntil(250, Combat::isSpecialAttackEnabled);
                    }

                }

                // 2. Fight back pker if not
                if (pker != null) {
                    if (Query.players().nameEquals(pker.getName()).isMyPlayerNotInteractingWith().isAny()) {
                        pker.click();
                    }

                    DetectPlayerThread.handleEatAndPrayer(pker);
                }
                }

            try {
                Thread.sleep(50);
            }catch (Exception e) {
                Log.error(e);
            }
        }
    }
}
