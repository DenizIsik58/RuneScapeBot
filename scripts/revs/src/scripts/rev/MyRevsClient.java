package scripts.rev;

import org.tribot.script.sdk.Bank;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.MyPlayer;
import org.tribot.script.sdk.Prayer;
import org.tribot.script.sdk.types.Area;
import org.tribot.script.sdk.types.WorldTile;
import scripts.api.MyScriptVariables;
import scripts.api.utility.StringsUtility;

public class MyRevsClient {

    private static RevScript script = null;

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



    public static void processMessage(String message) {

        if (message.contains("giving it a total of")) {
            var chargeString = StringsUtility.extractLastMatch("\\d+", message).orElse("");
            Log.info("CHARGES: " + chargeString);
            if (!chargeString.isEmpty()) EquipmentManager.setBowCharges(Integer.parseInt(chargeString));
        }

        if (message.equals("<col=ef1020>Your weapon has run out of revenant ether.</col>")){

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


    public static RevScript getScript() {
        if (script == null) script = MyScriptVariables.getScript();
        return script;
    }





}
