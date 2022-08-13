package scripts.api;

import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.query.Query;

public class FoodManager {


    public static void eatFood(){
        var foodCount = Query.inventory().actionEquals("Eat").count();
        var ate=  Query.inventory()
                .actionEquals("Eat")
                .findClosestToMouse()
                .map(c -> c.click("Eat")
                        && Waiting.waitUntil(1000, () -> Query.inventory().actionEquals("Eat").count() < foodCount))
                .orElse(false);
        if (ate) {
            MyAntiBan.calculateNextEatPercent();
        }
    }
}
