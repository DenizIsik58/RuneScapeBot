package scripts.rev;

import org.tribot.script.sdk.GameTab;
import org.tribot.script.sdk.Skill;
import org.tribot.script.sdk.query.Query;

public class BoostingManager {

    private static boolean isBoosted = false;


    public static boolean isBoosted(){
        return Skill.RANGED.getCurrentLevel() != Skill.RANGED.getActualLevel();
    }

    public static void resetBoost() {

    }

    public static void boost() {
        Query.inventory().nameContains("Divine ranging potion").findClosestToMouse().ifPresent(pot -> {
            pot.click("Drink");
            GameTab.EQUIPMENT.open();
        });

    }

}
