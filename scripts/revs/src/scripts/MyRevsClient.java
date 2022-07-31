package scripts;

import org.tribot.script.sdk.MyPlayer;
import org.tribot.script.sdk.Prayer;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Area;
import org.tribot.script.sdk.types.WorldTile;

public class MyRevsClient {


    public static boolean myPlayerIsDead(){
        return Area.fromRectangle(new WorldTile(3217, 3223, 0), new WorldTile(3224, 3215, 0)).containsMyPlayer();
    }

    public static boolean myPlayerHasEnoughChargesInBow(){
        return EquipmentManager.getBowCharges() >= 200;
    }

    public static boolean myPlayerHasTooManyChargesInBow(){
        return EquipmentManager.getBowCharges() >= 1500;
    }

    public static boolean myPlayerHasEnoughChargesInBracelet(){
        return EquipmentManager.getBraceCharges() >= 100;
    }

    public static boolean myPlayerHasTooManyChargesInBrace(){
        return EquipmentManager.getBraceCharges() >= 700;
    }

    public static boolean myPlayerIsInGE(){
        return Area.fromRectangle(new WorldTile(3185, 3468, 0), new WorldTile(3140, 3513, 0)).containsMyPlayer();
    }

    public static boolean myPlayerIsInCave(){
        return Area.fromRectangle(new WorldTile(3135, 10045, 0), new WorldTile(3259, 10147, 0)).containsMyPlayer();
    }

    public static boolean myPlayerIsInFerox(){
        return Area.fromRectangle(new WorldTile(3161, 3641, 0), new WorldTile(3120, 3616, 0)).containsMyPlayer();
    }

    public static boolean myPlayerNeedsToRefresh(){
        return MyPlayer.getCurrentHealthPercent() < 80 || Prayer.getPrayerPoints() < 30 || MyPlayer.getRunEnergy() < 50;
    }



    public static boolean clickWidget(String action, int... indexPath) {
        return Query.widgets()
                .inIndexPath(indexPath)
                .actionContains(action)
                .findFirst()
                .map(widget -> widget.click(action))
                .orElse(false);
    }

    public static boolean isWidgetVisible(int... indexPath) {
        return Query.widgets()
                .inIndexPath(indexPath)
                .isVisible()
                .isAny();
    }


}
