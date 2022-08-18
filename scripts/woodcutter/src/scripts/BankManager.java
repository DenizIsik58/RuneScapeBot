package scripts;

import org.tribot.script.sdk.Inventory;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.MyPlayer;
import org.tribot.script.sdk.query.Query;
import scripts.api.MyBanker;

public class BankManager {


    public static void bankLogs(int axeId) {
        MyBanker.openBank();
        MyBanker.depositInventory();
        if (!Query.bank().idEquals(axeId).isAny()) {
            throw new RuntimeException("Couldn't find the axe that in your bank!");
        }
        MyBanker.withdraw(axeId, 1, false);
        MyBanker.closeBank();
    }

    public static void dropInventory(int axeId){
        Inventory.getAll().forEach(item -> {
            if (item.getId() != axeId) {
                item.click("Drop");
            }
        });
    }

    public static void bankToGearUp() {
        if (MyPlayer.getCombatLevel() >= 15) {
            Log.debug("You are above level 15. Skipping training");
            return;
        }
        Log.debug("Started gearing up process!");

        var open = MyBanker.openBank();
        if (!open) {
            Log.debug("Couldn't open bank. Trying again..");
            MyBanker.openBank();
        }
        if (MyBanker.isHelpOpen()) {
            MyBanker.closeHelp();
        }
        MyBanker.depositAll();

        WCEquipmentManager.gearUp();
        MyBanker.openBank();
        Query.bank().actionEquals("Eat").findFirst().ifPresent(food -> MyBanker.withdraw(food.getId(), 1, false));
        MyBanker.closeBank();
    }
}
