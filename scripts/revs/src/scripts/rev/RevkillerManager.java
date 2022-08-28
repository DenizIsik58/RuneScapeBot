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

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;


public class RevkillerManager {
    private static final int demonId = 7936;
    private static final int orkId = 7937;
    private static boolean iWasFirst = false;
    private static Npc target = null;
    private static int killCount = 0;
    private static int startRangeLevel = Skill.RANGED.getCurrentLevel();

    @Getter @Setter
    private static boolean hasClickedSpot = false;
    private static boolean checkedSupplies = false;

    private static AtomicBoolean bossDetected = new AtomicBoolean(false);

    private static AtomicBoolean lowFood = new AtomicBoolean(false);

    private static AtomicBoolean lowRestores = new AtomicBoolean(false);

    private static AtomicBoolean lowArrows = new AtomicBoolean(false);

    @Setter
    private static SuppliesChecker checker = null;
    public static void killMonster(){

        if (!MyRevsClient.getScript().isState(State.KILLING)) {
            Log.debug("It's not killing state! Returning!");
            return;
        }

        if (checker == null) {
            Log.debug("Supplies checker is null. Starting a new thread");
            checker = new SuppliesChecker();
            new Thread(checker).start();
        }

       /* if (Query.players().isNotEquipped(DetectPlayerThread.getPvmGear()).isAny() || Query.players().count() == 0) {
            iWasFirst = true;
        }*/

        //if (iWasFirst) {

        if (!iWasFirst) {
            if (pvmers().count() < 2) {
                iWasFirst = true;
            }
        }

        if (iWasFirst && Combat.isInWilderness()){

            if (LootingManager.hasPkerBeenDetected()) {
                return;
            }

            if (LootingManager.hasLootBeenDetected()){
                Log.debug("Found loot! Switching to looting statw!");
                MyRevsClient.getScript().setState(State.LOOTING);
                return;
            }

            if (lowRestores.get()) {
                Log.debug("Low of restore");

                TeleportManager.teleportOut();
                //TeleportManager.teleportOutOfWilderness("We are low on prayer. trying to teleport out..");
                MyRevsClient.getScript().setState(State.BANKING);
                return;
            }

            if (bossDetected.get()) {
                TeleportManager.teleportOut();
                MyRevsClient.getScript().setState(State.BANKING);
                return;
            }

            if (lowFood.get()) {
                Log.debug("Low on food");

                TeleportManager.teleportOut();

                //TeleportManager.teleportOutOfWilderness("We are low on food. trying to teleport out..");
                MyRevsClient.getScript().setState(State.BANKING);
                return;
            }

            if (lowArrows.get()) {

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

            // TODO: Should probably have a method to open the looting bag.
            LootingManager.openLootingBag();


            if (Prayer.getPrayerPoints() < Skill.PRAYER.getActualLevel() - 22) {
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
                        Waiting.waitNormal(500, 100);
                    }
                });

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

        Waiting.wait(100);

       /* }else {
            if ((Query.players().isEquipped("Black d'hide body", "Toxic blowpipe", "Magic shortbow", "Magic shortbow (i)", "Craw's bow", "Viggora's chainmace").isAny() || Query.players().isBeingInteractedWith().isAny() || Query.players().isHealthBarVisible().isAny()) && !iWasFirst) {
                // Hop worlds
            }
        }*/
    }

    private static boolean hasCheckedSupplies() {
        return checkedSupplies;
    }

    public static void setCheckedSupplies(boolean checkedSupplies) {
        RevkillerManager.checkedSupplies = checkedSupplies;
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

    public static void setLowFood(boolean lowFood) {
        RevkillerManager.lowFood.set(lowFood);
    }

    public static void setLowArrows(boolean lowFood) {
        RevkillerManager.lowArrows.set(lowFood);
    }

    public static void setBossDetected(boolean lowFood) {
        RevkillerManager.bossDetected.set(lowFood);
    }

    public static void setLowRestores(boolean lowFood) {
        RevkillerManager.lowRestores.set(lowFood);
    }
}
