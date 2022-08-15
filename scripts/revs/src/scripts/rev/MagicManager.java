package scripts.rev;


import org.tribot.script.sdk.Combat;
import org.tribot.script.sdk.MyPlayer;
import org.tribot.script.sdk.Prayer;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.query.Query;

public class MagicManager implements Runnable {


    @Override
    public void run() {
        while (MyRevsClient.getScript().getPlayerDetectionThread() != null) {
            var pker = DetectPlayerThread.getPker();
            DetectPlayerThread.setEntangle();

            if (DetectPlayerThread.isFrozen()) {
                // Else
                // Do antipk here
                PrayerManager.enablePrayer(Prayer.PROTECT_ITEMS);
                PrayerManager.enablePrayer(Prayer.EAGLE_EYE);
                if (Combat.canUseSpecialAttack()) {
                    if (MyPlayer.getCurrentHealthPercent() < 70) {
                        Waiting.waitUntil(250, Combat::activateSpecialAttack);
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

            Waiting.wait(50);
        }
    }
}
