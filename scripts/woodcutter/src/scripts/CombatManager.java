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
        if (MyPlayer.get().get().getEquippedItem(Equipment.Slot.WEAPON).isPresent() && MyPlayer.get().get().getEquippedItem(Equipment.Slot.SHIELD).isPresent()) {
            return;
        }
        if(!Bank.isNearby()) {
            GlobalWalking.walkToBank();
            if (!Bank.isOpen()){
                Bank.open();
                Bank.depositInventory();
                Bank.withdraw("Bronze sword", 1);
                Bank.withdraw("Wooden shield", 1);
                Bank.withdraw("Bread", 1);
                Bank.withdraw("Shrimps", 1);
                Bank.close();
                Inventory.getAll().forEach(item -> item.click("Wield"));
            }
        }
    }

    public static void train(){
        while (MyPlayer.getCombatLevel() != 8){

            if (!chickenPlace.isVisible()) {
                GlobalWalking.walkTo(chickenPlace);
            }

            if (!Combat.isAttackStyleAvailable(Combat.AttackStyle.AGGRESSIVE) && Combat.getCurrentAttackStyle() != Combat.AttackStyle.AGGRESSIVE) {
                Combat.setAttackStyle(Combat.AttackStyle.AGGRESSIVE);
            }

            if (MyPlayer.getCurrentHealthPercent() < 20) {
                Inventory.getAll().forEach(item -> item.click("Eat"));
            }

            Query.groundItems().nameEquals("Feather").forEach(groundItem -> groundItem.click("Take"));

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
