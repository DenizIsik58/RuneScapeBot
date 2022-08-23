package scripts.rev;

import lombok.Getter;
import lombok.Setter;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.PlayerQuery;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Npc;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import org.tribot.script.sdk.walking.WalkState;
import scripts.api.FoodManager;
import scripts.api.MyAntiBan;
import scripts.api.MyCamera;
import scripts.api.MyScriptVariables;
import scripts.api.utility.MathUtility;



public class RevkillerManager {
    private static final int demonId = 7936;
    private static final int orkId = 7937;
    private static boolean iWasFirst = false;
    private static Npc target = null;
    private static int killCount = 0;
    private static int startRangeLevel = Skill.RANGED.getCurrentLevel();

    @Getter @Setter
    private static boolean hasClickedSpot = false;

    public static void killMonster(){

        if (!MyRevsClient.getScript().isState(State.KILLING)) {
            Log.debug("It's not killing state! Returning!");
            return;
        }

        if (!GameTab.EQUIPMENT.isOpen()) {
            GameTab.EQUIPMENT.open();
        }


        Query.npcs().nameEquals("Revenant maledictus").findFirst().ifPresent(boss -> {
            if (boss.isValid() || boss.isAnimating() || boss.isMoving() || boss.isHealthBarVisible() || boss.getTile().isVisible() || boss.getTile().isRendered()){
                //TeleportManager.teleportOutOfWilderness("Boss has been seen! Trying to teleport out");
                    TeleportManager.teleportOut();

            }
        });

        if (Combat.isAutoRetaliateOn()){
            Combat.setAutoRetaliate(false);
        }

        if (Options.AttackOption.getNpcAttackOption() != Options.AttackOption.LEFT_CLICK_WHERE_AVAILABLE){
            Options.AttackOption.setNpcAttackOption(Options.AttackOption.LEFT_CLICK_WHERE_AVAILABLE);
        }

       /* if (Query.players().isNotEquipped(DetectPlayerThread.getPvmGear()).isAny() || Query.players().count() == 0) {
            iWasFirst = true;
        }*/

        //if (iWasFirst) {

        if (pvmers().count() < 2) {
            iWasFirst = true;
        }




        if (iWasFirst && Combat.isInWilderness()){


            if (LootingManager.hasPkerBeenDetected()) {
                return;
            }

            if (Query.groundItems().isAny() && LootingManager.hasLootBeenDetected()){
                Log.debug("Found loot! Switching to looting statw!");
                MyRevsClient.getScript().setState(State.LOOTING);
                return;
            }

            if (Query.inventory().nameContains("Blighted super restore").count() == 0) {
                Log.debug("Low of restore");

                    TeleportManager.teleportOut();


                //TeleportManager.teleportOutOfWilderness("We are low on prayer. trying to teleport out..");
                MyRevsClient.getScript().setState(State.BANKING);
                return;
            }

            if (Query.inventory().actionEquals("Eat").count() < 6) {
                Log.debug("Low on food");

                    TeleportManager.teleportOut();

                //TeleportManager.teleportOutOfWilderness("We are low on food. trying to teleport out..");
                MyRevsClient.getScript().setState(State.BANKING);
                return;
            }
            if (Equipment.getCount(892) < 10) {

                Log.debug("Low on arrows teleporting out.");
                    TeleportManager.teleportOut();
                    MyRevsClient.getScript().setState(State.BANKING);
                    return;

            }

            if (!MyRevsClient.myPlayerIsAtSouthOrk()) {
                GlobalWalking.walkTo(MyRevsClient.getScript().getSelectedMonsterTile());
            }

            PrayerManager.enableQuickPrayer();
            MyCamera.init();

            if (hasLevelGained()){
                MyScriptVariables.setRangedLevelString(MathUtility.getRangeLevelRate(startRangeLevel, Skill.RANGED.getActualLevel()));
            }

            if (!Query.equipment().nameContains("Ring of wealth (").isAny()){
                Log.debug("Cannot find wealth in equipment. Checking inventory");
                Query.inventory().nameContains("Ring of wealth (").findClosestToMouse().ifPresent(ring -> {
                    Log.debug("found it! Wearing it now!");
                    ring.click("Wear");
                    Waiting.waitUntil(() -> Query.equipment().slotEquals(Equipment.Slot.RING).nameContains("Ring of wealth (").isAny());
                });
            }



            // TODO: Should probably have a method to open the looting bag.
            var lootingBag = Query.inventory().nameEquals("Looting bag").findFirst().orElse(null);

            if (lootingBag != null) {
                if (lootingBag.getId() == 11941) {
                    Waiting.waitUntil(() -> lootingBag.click("Open"));
                    Waiting.waitUntil(() -> Inventory.contains(22586));
                    if (target != null) {
                        target.click();
                    }
                }
            }


            if (Prayer.getPrayerPoints() < 24) {
                PrayerManager.maintainPrayerPotion();
                if (target != null){
                    target.click();
                }
            }

            if (!BoostingManager.isBoosted()){
                BoostingManager.boost();
                if (target != null){
                    target.click();
                }
            }

            if (!MyRevsClient.myPlayerHas40Defence()) {
                if (!Combat.isAttackStyleSet(Combat.AttackStyle.LONGRANGE)) {
                    Combat.setAttackStyle(Combat.AttackStyle.LONGRANGE);
                    Waiting.waitUntil(() -> Combat.isAttackStyleSet(Combat.AttackStyle.LONGRANGE));
                }
            }else {
                if (!Combat.isAttackStyleSet(Combat.AttackStyle.RAPID)) {
                    Combat.setAttackStyle(Combat.AttackStyle.RAPID);
                }
            }


            if (MyAntiBan.shouldEat()) {
                FoodManager.eatFood();
                if (target != null){
                    target.click();
                }
            }

            if (target == null){
                target = TargetManager.chooseNewTarget(TeleportManager.getMonsterIdBasedOnLocation(MyRevsClient.getScript().getSelectedMonsterTile()));
            }

            if (target != null) {
                Query.npcs().idEquals(TeleportManager.getMonsterIdBasedOnLocation(MyRevsClient.getScript().getSelectedMonsterTile())).findRandom().ifPresent(monster -> {
                    if (monster.isInteractingWithMe() && !monster.isHealthBarVisible()){
                        target = monster;
                        monster.click();
                    }
                });


                if (!target.isVisible()){
                    target.adjustCameraTo();
                    target.click();
                }

                if (!target.isHealthBarVisible() || (target.getHealthBarPercent() != 0 && !target.isAnimating() && !target.isHealthBarVisible())){
                    target.click();
                    Waiting.waitUntil(2000, () -> target.isHealthBarVisible());
                }

                if((target.getHealthBarPercent() == 0 && Query.npcs().idEquals(TeleportManager.getMonsterIdBasedOnLocation(MyRevsClient.getScript().getSelectedMonsterTile())).isAny()) || !target.isValid() || (target.isHealthBarVisible() && !target.isInteractingWithMe())){
                    Log.debug("[WILDERNESS_LISTENER] target mob died. Finding a new one!");
                    incrementKillCounts();
                    MyScriptVariables.setKillCountString(String.valueOf(getKillCount()));
                    target = TargetManager.chooseNewTarget(TeleportManager.getMonsterIdBasedOnLocation(MyRevsClient.getScript().getSelectedMonsterTile()));
                }

            }


            if (LootingManager.getTripValue() >= 200000) {
                if (!MyRevsClient.myPlayerIsInGE()) {
                    Waiting.waitUntil(250, () -> new WorldTile(3205, 10082, 0).clickOnMinimap());
                    Waiting.wait(2000);
                    Equipment.Slot.RING.getItem().map(c -> c.click("Grand Exchange"));

                    MyRevsClient.getScript().setState(State.BANKING);
                }
            }
        }else {
            if (!Combat.isInWilderness()) {
                MyRevsClient.getScript().setState(State.BANKING);
                return;
            }

            if (MyPlayer.isHealthBarVisible()) {
                Log.debug("We are being attacked");
                return;
            }

            if (!WorldManager.hopToRandomMemberWorldWithRequirements()){
                Log.debug("We are being attacked!");
            }
        }


       /* }else {
            if ((Query.players().isEquipped("Black d'hide body", "Toxic blowpipe", "Magic shortbow", "Magic shortbow (i)", "Craw's bow", "Viggora's chainmace").isAny() || Query.players().isBeingInteractedWith().isAny() || Query.players().isHealthBarVisible().isAny()) && !iWasFirst) {
                // Hop worlds
            }
        }*/
    }


    private static PlayerQuery pvmers(){
        return Query.players().isEquipped(DetectPlayerThread.getPvmGear());
    }
    public static boolean hasLevelGained(){
        return startRangeLevel != Skill.RANGED.getCurrentLevel();
    }

    public static void incrementKillCounts() {
        killCount++;
    }

    public static int getKillCount() {
        return killCount;
    }

    public static void setStartRangeLevel(int startRangeLevel) {
        RevkillerManager.startRangeLevel = startRangeLevel;
    }

    public static Npc getTarget() {
        return target;
    }

    public static boolean isiWasFirst() {
        return iWasFirst;
    }


    public static void setiWasFirst(boolean iWasFirst) {
        RevkillerManager.iWasFirst = iWasFirst;
    }
}
