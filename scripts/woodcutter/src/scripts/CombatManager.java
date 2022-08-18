package scripts;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Npc;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import scripts.api.MyAntiBan;

public class CombatManager {

    private static final WorldTile chickenPlace = new WorldTile(3232, 3294, 0);
    private static Npc target;

    public static void killChickens(){
        BankManager.bankToGearUp();
        train();
    }

    public static void train(){
        while (MyPlayer.getCombatLevel() < 15){

            if (!chickenPlace.isVisible()) {
                GlobalWalking.walkTo(chickenPlace);
            }

            if ((Skill.STRENGTH.getCurrentLevel() < 15)) {
                if (!Combat.isAttackStyleSet(Combat.AttackStyle.AGGRESSIVE)) {
                    Combat.setAttackStyle(Combat.AttackStyle.AGGRESSIVE);
                }
            }else {
                if (!Combat.isAttackStyleSet(Combat.AttackStyle.DEFENSIVE)) {
                    Combat.setAttackStyle(Combat.AttackStyle.DEFENSIVE);
                }
            }

            if (MyAntiBan.shouldEat()) {
                //Take 1 food
                Query.inventory().actionEquals("Eat").findClosestToMouse().ifPresent(food -> food.click("Eat"));
                MyAntiBan.calculateNextEatPercent();
            }

            // rework this
            if (target == null) {
                assignNewTarget();
            }

            if (target.getHealthBarPercent() == 0 || !target.isValid()) {
                assignNewTarget();
            }

            if (!target.isHealthBarVisible() && !target.isAnimating()) {
                target.click("Attack");
            }
        }
    }

    private static void assignNewTarget(){
            Query.npcs()
                    .nameEquals("Chicken")
                    .findBestInteractable()
                    .ifPresent(chicken -> target = chicken);
    }

}
