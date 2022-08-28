package scripts.rev;

import org.tribot.script.sdk.Combat;
import org.tribot.script.sdk.Equipment;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.query.Query;

public class SuppliesChecker implements Runnable{
    @Override
    public void run() {

        while (MyRevsClient.getScript().getPlayerDetectionThread() != null && Combat.isInWilderness()) {
            if (Query.inventory().nameContains("Blighted super restore").count() == 0) {
                Log.debug("Low of restore");
                RevkillerManager.setLowRestores(true);

                return;
            }

            Query.npcs().nameEquals("Revenant maledictus").findRandom().ifPresent(boss -> {
                if (boss.isValid() || boss.isAnimating() || boss.isMoving() || boss.isHealthBarVisible()) {
                    //TeleportManager.teleportOutOfWilderness("Boss has been seen! Trying to teleport out");
                    RevkillerManager.setBossDetected(true);

                }
            });

            if (Query.inventory().actionEquals("Eat").count() < 6) {
                Log.debug("Low on food");
                RevkillerManager.setLowFood(true);

                return;
            }

            if (Equipment.getCount(892) < 10) {
                RevkillerManager.setLowArrows(true);
                return;

            }

            Waiting.wait(5000);
        }
    }
}
