package scripts;

import org.tribot.script.sdk.Bank;
import org.tribot.script.sdk.Inventory;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.World;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;

import java.util.List;

public class BankManager {



    public static void bank(String currentAxe) {

        if (!Bank.isNearby()){
            GlobalWalking.walkToBank();
        }else {
            if (!Bank.isOpen()){
                Bank.open();
                if (isHelpOpen()) {
                    closeHelp();
                }
            }
            Bank.depositInventory();
            Bank.withdraw(currentAxe, 1);
            Bank.close();
        }
    }



    public static void bankToGearUp() {
        if(!Bank.isNearby()) {
            GlobalWalking.walkToBank();
            if (!Bank.isOpen()){
                Bank.open();
            }
                if (isHelpOpen()) {
                    closeHelp();
                }
                Bank.depositInventory();
                Bank.withdraw("Bronze sword", 1);
                Bank.withdraw("Wooden shield", 1);
                Bank.withdraw("Bread", 1);
                Bank.withdraw("Shrimps", 1);
                Bank.close();
                Inventory.getAll().forEach(item -> item.click("Wield"));
        }
    }

    private static void closeHelp() {
        MyClient.clickWidget("Close", 664, 29, 0);
    }
    private static boolean isHelpOpen() {
        return MyClient.isWidgetVisible(664, 8);
    }

}
