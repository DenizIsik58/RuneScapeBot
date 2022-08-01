package scripts;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Area;
import org.tribot.script.sdk.types.Widget;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.util.Retry;

import java.util.Optional;

public class MyRevsClient {


    public static boolean myPlayerIsDead(){
        return Area.fromRectangle(new WorldTile(3217, 3226, 0), new WorldTile(3226, 3211, 0)).containsMyPlayer();
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
        return Area.fromRectangle(new WorldTile(3140, 3513, 0), new WorldTile(3189, 3467, 0)).containsMyPlayer();
    }

    public static boolean myPlayerIsInCave(){
        return Area.fromRectangle(new WorldTile(3136, 10142, 0), new WorldTile(3270, 10053, 0)).containsMyPlayer();
    }

    public static boolean myPlayerIsInFerox(){
        return Area.fromRectangle(new WorldTile(3156, 3644, 0), new WorldTile(3119, 3602, 0)).containsMyPlayer();
    }

    public static boolean myPlayerNeedsToRefresh(){
        return MyPlayer.getCurrentHealthPercent() < 80 || Prayer.getPrayerPoints() < 30 || MyPlayer.getRunEnergy() < 50 || MyPlayer.isPoisoned();
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

    public static void processMessage(String message) {
        if (message.equals("<col=ef1020>Your weapon has run out of revenant ether.</col>")){
            PkerDetecter.quickTele();
            return;
        }
        if (message.equals("<col=ef1020>The effects of the divine potion have worn off.")){
            BoostingManager.resetBoost();
            return;
        }
        if (message.equals("You don't have enough inventory space.")){
            Bank.depositInventory();
            BankManagerRevenant.withdrawGear();
        }

        try {
            var content = message.split(" ");
            var type = content[1];
            if (type.equals("bracelet")) {
                if (message.contains("it will not absorb")){
                    EquipmentManager.toggleBraceletAbsorbOn();
                }
                // Update bracelet charges
                EquipmentManager.setBraceCharges(Integer.parseInt(content[3]));
            } else if (type.equals("bow")) {
                // update bow charges
                EquipmentManager.setBowCharges(Integer.parseInt(content[3].replace(",", "")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static boolean waitUntilLoggedIn() {
        boolean success = Retry.retry(5, () -> {
            if (isClickToPlayVisible()) clickClickToPlay();
            return Waiting.waitUntil(Login::isLoggedIn);
        });
        if (!success) {
            Login.login();
            waitUntilLoggedIn();
        }
        return Login.isLoggedIn();
    }

    //<editor-fold desc="waitUntilLoggedIn support methods">
    private static Optional<Widget> getClickToPlayButton() {
        return Query.widgets().inIndexPath(378, 78).findFirst();
    }
    private static boolean isClickToPlayVisible() {
        return getClickToPlayButton().map(Widget::isVisible).orElse(false);
    }
    private static void clickClickToPlay() {
        getClickToPlayButton().ifPresent(button -> button.click("Play"));
    }
    //</editor-fold>

}
