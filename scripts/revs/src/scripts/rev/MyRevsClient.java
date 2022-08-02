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

    private static final String TELEBLOCK_REGEX ="A Tele Block spell has been cast on you by (.*?)\\.";
    private static final String TELEBLOCK_MESSAGE_REGEX = "A teleport block has been cast on you\\. It should wear off in ";

    private static RevScript script = null;

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
        return isInLocation(true, Area.fromRectangle(new WorldTile(3140, 3513, 0), new WorldTile(3189, 3467, 0)));
    }

    public static boolean myPlayerIsDead(){
        return isInLocation(true, Area.fromRectangle(new WorldTile(3217, 3226, 0), new WorldTile(3226, 3211, 0)));
    }

    public static boolean myPlayerIsInCave(){
        return Area.fromRectangle(new WorldTile(3136, 10142, 0), new WorldTile(3270, 10053, 0)).containsMyPlayer();
    }

    public static boolean myPlayerIsInFerox(){
        return isInLocation(true, Area.fromRectangle(new WorldTile(3156, 3644, 0), new WorldTile(3119, 3602, 0)));
    }

    public static boolean myPlayerNeedsToRefresh(){
        // Should probably do 100
        return MyPlayer.getCurrentHealthPercent() < 100 || Prayer.getPrayerPoints() < 43 || MyPlayer.getRunEnergy() < 100 || MyPlayer.isPoisoned();
    }

    private static boolean isInLocation(boolean resetTeleBlockIfTrue, Area area) {
        if (area.containsMyPlayer()) {
            if (resetTeleBlockIfTrue) resetTeleblock();
            return true;
        }
        return false;
    }

    private static void resetTeleblock() {
        MyScriptVariables.setVariable("lastTeleblockNotification", 0L);
    }


    public static void processMessage(String message) {

        if (StringsUtility.hasMatches(TELEBLOCK_MESSAGE_REGEX, message)) {
            MyScriptVariables.setVariable("lastTeleblockNotification", System.currentTimeMillis());
        }

        if (StringsUtility.hasMatches(TELEBLOCK_REGEX, message)) {
            var playerName = StringsUtility.extractFirstMatchGroup(TELEBLOCK_REGEX, message);
            if (playerName.isEmpty()) Log.warn("Teleblock Regex detected, but got an empty name? I confuse?");
            else {
                MyScriptVariables.setVariable("lastTeleblockNotification", System.currentTimeMillis());
                Log.warn("Teleblocked by some cunt named: " + playerName + "!!!");
                MyScriptVariables.setVariable("pkerName", playerName);
            }
        }

        if (message.contains("giving it a total of")) {
            var chargeString = StringsUtility.extractLastCompleteMatch("\\d+", message);
            Log.info("CHARGES: " + chargeString);
            if (!chargeString.isEmpty()) EquipmentManager.setBowCharges(Integer.parseInt(chargeString));
        }

        if (message.equals("<col=ef1020>Your weapon has run out of revenant ether.</col>")){
            TeleportManager.teleportOutOfWilderness("Teleporting out. We are out of ether.");
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
