package scripts.rev;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.types.Area;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import org.tribot.script.sdk.walking.WalkState;
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

    public static boolean myPlayerIsInCasteWars(){
        return Area.fromRectangle(new WorldTile(2435, 3103, 0), new WorldTile(2456, 3073, 0)).containsMyPlayer();
    }

    public static boolean myPlayerIsInWhitePortal(){
        return Area.fromRectangle(new WorldTile(3316, 4761, 0), new WorldTile(3341, 4746, 0)).containsMyPlayer();
    }

    public static boolean myPlayerIsInGE(){
        return isInLocation(true, Area.fromRectangle(new WorldTile(3131, 3518, 0), new WorldTile(3201, 3457, 0)));
    }

    public static boolean myPlayerIsAtEastGoblin(){
        return Area.fromRectangle(new WorldTile(3234, 10110, 0), new WorldTile(3251, 10082, 0)).containsMyPlayer();
    }

    public static boolean myPlayerIsDead(){
        return isInLocation(true, Area.fromRectangle(new WorldTile(3217, 3226, 0), new WorldTile(3226, 3211, 0)));
    }

    public static boolean myPlayerIsAtEdge(){
        return Area.fromRectangle(new WorldTile(3066, 3519, 0), new WorldTile(3116, 3462, 0)).containsMyPlayer();
    }

    public static boolean myPlayerIsAtSouthOrk(){
        return Area.fromRectangle(new WorldTile(3200, 10105, 0), new WorldTile(3231, 10085, 0)).containsMyPlayer();
    }

    public static boolean myPlayerIsInCave(){
        return Area.fromRectangle(new WorldTile(3136, 10142, 0), new WorldTile(3270, 10053, 0)).containsMyPlayer();
    }

    public static boolean myPlayerIsInFerox(){
        return isInLocation(true, Area.fromRectangle(new WorldTile(3156, 3644, 0), new WorldTile(3119, 3602, 0)));
    }

    public static boolean myPlayerNeedsToRefresh(){
        // Should probably do 100
        return MyPlayer.getCurrentHealthPercent() < 100 || Prayer.getPrayerPoints() < 43 || MyPlayer.getRunEnergy() <= 80 || MyPlayer.isPoisoned();
    }

    public static boolean myPlayerHas40Defence(){
        return Skill.DEFENCE.getActualLevel() >= 40;
    }

    private static boolean isInLocation(boolean resetTeleBlockIfTrue, Area area) {
        if (area.containsMyPlayer()) {
            if (resetTeleBlockIfTrue) {
                resetTeleblock();
            }
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
            if (playerName.isEmpty()) {
                Log.warn("Teleblock Regex detected, but got an empty name? I confuse?");
            } else {
                MyScriptVariables.setVariable("lastTeleblockNotification", System.currentTimeMillis());
                Log.warn("Teleblocked by some cunt named: " + playerName + "!!!");
                MyScriptVariables.setVariable("pkerName", playerName);
                if (MyRevsClient.getScript().getPlayerDetectionThread() != null) {
                    MyRevsClient.getScript().getPlayerDetectionThread().setTeleblocked(true);
                }
            }
        }

        if (message.contains("giving it a total of")) {
            var chargeString = StringsUtility.extractLastCompleteMatch("\\d+", message);
            Log.info("CHARGES: " + chargeString);
            if (!chargeString.isEmpty()) EquipmentManager.setBowCharges(Integer.parseInt(chargeString));
        }
        if (message.equals("There is not enough revenant ether left powering your bow.")) {
            Log.debug("out of ether teleporting out");
            TeleportManager.teleportOut();
            return;
        }

        if (message.equals("<col=ef1020>Your weapon has run out of revenant ether.</col>")){
            var location = new WorldTile(3205, 10082, 0);
            GlobalWalking.walkTo(location,  () -> {
                if ((LootingManager.hasPkerBeenDetected() && !Combat.isInWilderness()) || location.isOnMinimap()) {
                    return WalkState.FAILURE;
                }
                return WalkState.CONTINUE;
            });

            Waiting.wait(2000);
            Equipment.Slot.RING.getItem().ifPresent(c -> c.click("Grand Exchange"));

            return;
        }

        if (message.equals("<col=4f006f>Your Tele Block has expired.")) {
            Log.debug("Our teleblock spell has expired");
            MyScriptVariables.setVariable("lastTeleblockNotification", 0L);
            return;
        }
        if (message.equals("<col=ef1020>The effects of the divine potion have worn off.")){
            Log.debug("Divine pot expired");
            //BoostingManager.resetBoost();
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
