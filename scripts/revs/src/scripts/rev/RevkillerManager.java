package scripts.rev;

import lombok.Getter;
import lombok.Setter;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.types.Npc;
import org.tribot.script.sdk.walking.GlobalWalking;
import org.tribot.script.sdk.walking.WalkState;
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

        if (!GameTab.EQUIPMENT.isOpen()) {
            GameTab.EQUIPMENT.open();
        }


        var boss = Query.npcs().nameEquals("Revenant maledictus").findFirst().orElse(null);

        if (boss != null){
            if (boss.isValid() || boss.isAnimating() || boss.isMoving() || boss.isHealthBarVisible() || boss.getTile().isVisible() || boss.getTile().isRendered()){
                TeleportManager.teleportOutOfWilderness("Boss has been seen! Trying to teleport out");
                MyRevsClient.getScript().setState(State.BANKING);
                return;

            }
        }

        if (Combat.isAutoRetaliateOn()){
            Combat.setAutoRetaliate(false);
        }
        if (Query.players().count() == 0) {
            iWasFirst = true;
        }

        if (iWasFirst) {
            if (!MyRevsClient.getScript().getSelectedMonsterTile().isVisible()){
                GlobalWalking.walkTo(MyRevsClient.getScript().getSelectedMonsterTile());
            }
            PrayerManager.enableQuickPrayer();
            MyCamera.setCameraAngle();
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

            if (Query.inventory().nameContains("Prayer potion").count() == 0) {
                TeleportManager.teleportOutOfWilderness("We are low on prayer. trying to teleport out..");
                MyRevsClient.getScript().setState(State.BANKING);
                return;
            }

            if (Query.inventory().nameEquals("Shark").count() < 4) {
                if (target != null){
                    if (target.isValid()){
                        target.interact("Attack");
                        Waiting.waitUntil(15000, () -> !target.isValid());
                        Waiting.waitNormal(5000, 500);
                        if (Query.groundItems().isAny() && LootingManager.hasLootBeenDetected()){
                            MyRevsClient.getScript().setState(State.LOOTING);
                        }
                    }
                }
                TeleportManager.teleportOutOfWilderness("We are low on shark. Trying to teleport out...");
                return;
            }

            // TODO: Should probably have a method to open the looting bag.
            var lootingBag = Query.inventory().nameEquals("Looting bag").findFirst().orElse(null);

            if (lootingBag != null) {
                if (lootingBag.getId() == 11941) {
                    lootingBag.click("Open");
                }
            }

            if (Prayer.getPrayerPoints() < 15) {
                PrayerManager.maintainPrayerPotion();
                if (target != null){
                    target.click();
                }
                Waiting.wait(1500);
            }

            if (!BoostingManager.isBoosted()){
                BoostingManager.boost();
                if (target != null){
                    target.click();
                }
                Waiting.wait(2000);
            }

            if (!Combat.isAttackStyleSet(Combat.AttackStyle.RAPID)) {
                Combat.setAttackStyle(Combat.AttackStyle.RAPID);
            }

            if (MyPlayer.getCurrentHealthPercent() < 70) {
                Query.inventory().nameEquals("Shark").findClosestToMouse().map(InventoryItem::click);
                if (target != null){
                    target.click();
                }
                Waiting.wait(1500);
            }



            if (target == null && !hasClickedSpot){
                GlobalWalking.walkTo(MyRevsClient.getScript().getSelectedMonsterTile(), () -> {
                    if (LootingManager.hasPkerBeenDetected()){
                        MyRevsClient.getScript().setState(State.BANKING);
                        return WalkState.FAILURE;
                    }
                    return WalkState.CONTINUE;
                });
                setHasClickedSpot(true);
                target = TargetManager.chooseNewTarget(TeleportManager.getMonsterIdBasedOnLocation(MyRevsClient.getScript().getSelectedMonsterTile()));
            }

            if (target != null) {
                var monster = Query.npcs().idEquals(TeleportManager.getMonsterIdBasedOnLocation(MyRevsClient.getScript().getSelectedMonsterTile())).findRandom().orElse(null);
                if (monster != null){
                    if (monster.isInteractingWithMe() && !monster.isHealthBarVisible()){
                        monster.adjustCameraTo();
                        monster.click();
                    }
                }

                if (!target.isVisible()){
                    GlobalWalking.walkTo(MyRevsClient.getScript().getSelectedMonsterTile());
                    target.adjustCameraTo();
                    target.click();
                }

                if (!target.isHealthBarVisible()){
                    target.click();
                    Waiting.waitUntil(500, () -> target.isHealthBarVisible());
                }


                if (target.isValid()){
                    if (target.getHealthBarPercent() != 0 && !target.isAnimating() && !target.isHealthBarVisible()) {
                        if (!target.isVisible()){
                            target.adjustCameraTo();
                        }
                        target.click();
                    }
                }

                if((target.getHealthBarPercent() == 0 && Query.npcs().idEquals(TeleportManager.getMonsterIdBasedOnLocation(MyRevsClient.getScript().getSelectedMonsterTile())).isAny()) || !target.isValid() || (target.isHealthBarVisible() && !target.isInteractingWithMe())){
                    Log.debug("[WILDERNESS_LISTENER] target mob died. Finding a new one!");
                    incrementKillCounts();
                    MyScriptVariables.setKillCountString(String.valueOf(getKillCount()));
                    target = TargetManager.chooseNewTarget(TeleportManager.getMonsterIdBasedOnLocation(MyRevsClient.getScript().getSelectedMonsterTile()));
                }

            }
            if (Query.groundItems().isAny() && LootingManager.hasLootBeenDetected()){
                MyRevsClient.getScript().setState(State.LOOTING);
                return;
            }

            if (LootingManager.getTripValue() >= 500000) {
                TeleportManager.teleportOutOfWilderness("We have above 500k gold. Trying to teleport out...");
                MyRevsClient.getScript().setState(State.BANKING);
            }

        }else {
            if ((Query.players().isEquipped("Black d'hide body", "Toxic blowpipe", "Magic shortbow", "Magic shortbow (i)", "Craw's bow", "Viggora's chainmace").isAny() || Query.players().isBeingInteractedWith().isAny() || Query.players().isHealthBarVisible().isAny()) && !iWasFirst) {
                // Hop worlds
                WorldManager.hopToRandomMemberWorldWithRequirements();
            }
        }
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
