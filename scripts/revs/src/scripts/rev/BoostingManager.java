package scripts.rev;

import org.tribot.script.sdk.GameTab;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.InventoryItem;

public class BoostingManager {

    private static boolean isBoosted = false;


    public static boolean isBoosted(){
        return isBoosted;
    }

    public static void resetBoost() {
        BoostingManager.isBoosted = false;
    }

    public static void boost() {
        BoostingManager.isBoosted = true;
        Query.inventory().nameContains("Divine ranging potion").findClosestToMouse().map(InventoryItem::click);
        GameTab.EQUIPMENT.open();
        Waiting.wait(2000);
        Query.inventory().nameEquals("Shark").findFirst().map(InventoryItem::click);
    }

}
