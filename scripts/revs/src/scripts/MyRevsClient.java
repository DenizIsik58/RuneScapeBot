package scripts;

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

    public static boolean myPlayerHasEnoughChargesInBracelet(){
        return EquipmentManager.getBraceCharges() >= 100;
    }

    public static boolean myPlayerIsInGE(){
        return Area.fromRectangle(new WorldTile(3185, 3468, 0), new WorldTile(3140, 3513, 0)).containsMyPlayer();
    }

    public static boolean myPlayerIsInFerox(){
        return Area.fromRectangle(new WorldTile(3155, 3640, 0), new WorldTile(3116, 3623, 0)).containsMyPlayer();
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
