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
                    assert pker != null;
                    if (pker.getHealthBarPercent() < 40) {
                        Log.debug("Pker has less than 40 percent health I'm speccing!");
                        if (!Combat.isSpecialAttackEnabled()) {
                            Combat.activateSpecialAttack();
                            Waiting.waitUntil(250, Combat::isSpecialAttackEnabled);
                        }

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

            assert pker != null;
            if (pker.getHealthBarPercent() == 0) {
                try {
                    var screenshot = ScreenShotManager.takeScreenShotAndSave("kills");

                    MyRevsClient.getScript().getPkKills().setUsername("Pk")
                            .setContent("@everyone **" + MyPlayer.getUsername() + "** has just killed - **" + pker.getName() + "**")
                            .addFile(screenshot)
                            .execute();
                } catch (Exception e) {
                    Log.error(e);
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
