package scripts;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;

public class CombatManager {

    private static final WorldTile chickenPlace = new WorldTile(3232, 3294, 0);


    public static void killChickens(){
        gearUp();
        train();
    }

    public static void gearUp(){
        if (MyPlayer.getCombatLevel() >= 15) {
            return;
        }

        if (MyPlayer.get().get().getEquippedItem(Equipment.Slot.WEAPON).isPresent() && MyPlayer.get().get().getEquippedItem(Equipment.Slot.SHIELD).isPresent()) {
            return;
        }

       BankManager.bankToGearUp();
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

            if (MyPlayer.getCurrentHealthPercent() < 20) {
                Inventory.getAll().forEach(item -> item.click("Eat"));
            }

            Query.groundItems().nameEquals("Feather").forEach(groundItem -> groundItem.interact("Take"));

            if (!MyPlayer.isAnimating() && !MyPlayer.isMoving()){
                if (!MyPlayer.isHealthBarVisible()) {
                    var chicken = Query.npcs().nameContains("Chicken")
                            .isHealthBarNotVisible()
                            .isNotBeingInteractedWith()
                            .isReachable()
                            .findBestInteractable();
                    if (chicken.map(c->c.interact("Attack")).orElse(false));
                    Waiting.wait(3500);
                }

            }
        }
    }

}
