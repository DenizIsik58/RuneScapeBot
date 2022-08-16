package scripts.rev;

import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Widget;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import scripts.api.MyBanker;
import scripts.api.MyClient;
import scripts.api.MyExchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DecantManager {
    private static final List<String> potions = new ArrayList<>(Arrays.asList(
            "Divine ranging potion(1)","Divine ranging potion(2)","Divine ranging potion(3)","Divine ranging potion(4)",
            "Stamina potion(1)","Stamina potion(2)","Stamina potion(3)","Stamina potion(4)",
            "Blighted super restore(1)","Blighted super restore(2)","Blighted super restore(3)","Blighted super restore(4)",
            "Saradomin brew(1)","Saradomin brew(2)","Saradomin brew(3)","Saradomin brew(4)"
    ));


    public static void decantPotionsFromBank(){

        MyExchange.walkToGrandExchange();
        MyBanker.openBank();
        MyBanker.depositInventory();

        for (var item: potions){
            Query.bank().nameEquals(item).findFirst().ifPresent(pot -> {
                MyBanker.withdraw(pot.getName(), 100000, true);
            });
        }

        MyBanker.closeBank();

        GlobalWalking.walkTo(new WorldTile(3157, 3481, 0));
        Query.npcs().idEquals(5449).findBestInteractable().map(c -> c.interact("Decant"));
        Waiting.waitUntil(() -> MyClient.isWidgetVisible(582, 6));
        Query.widgets()
                .inIndexPath(582, 6)
                .findFirst()
                .map(Widget::click);
        MyBanker.openBank();
        MyBanker.depositInventory();

    }
}
