package scripts;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.types.Npc;
import org.tribot.script.sdk.walking.GlobalWalking;

import java.lang.annotation.Target;

public class RevkillerManager {
    private static int demonId = 7936;
    private static int orkId = 7937;
    private static boolean iWasFirst = false;
    private static Npc target = null;

    public static void killMonster(){


        if (!GameTab.EQUIPMENT.isOpen()) {
            GameTab.EQUIPMENT.open();
        }
        var boss = Query.npcs().nameEquals("Revenant maledictus").findFirst().orElse(null);

        if (boss != null){
            if (boss.isValid() || boss.isAnimating() || boss.isMoving() || boss.isHealthBarVisible() || boss.getTile().isVisible() || boss.getTile().isRendered()){
                Log.info("Boss has been seen!");
                PkerDetecter.quickTele();
            }
        }
        if (Query.players().count() == 0) {
            iWasFirst = true;
        }

        if (Query.groundItems().isAny() && LootingManager.hasLootBeenDetected()){
            RevenantScript.state = State.LOOTING;
        }

        if (iWasFirst) {
            PrayerManager.enableQuickPrayer();
            CameraManager.setCameraAngle();

            if (Query.inventory().nameContains("Prayer potion").count() == 0) {
                Query.equipment().nameContains("Ring of wealth (").findFirst().map(ring -> ring.click("Grand exchange"));
                RevenantScript.state = State.BANKING;
                PrayerManager.disableQuickPrayer();
            }

            if (Query.inventory().nameEquals("Shark").count() < 4) {
                Query.equipment().nameContains("Ring of wealth (").findFirst().map(ring -> ring.click("Grand exchange"));
                RevenantScript.state = State.BANKING;
                PrayerManager.disableQuickPrayer();
            }

            var lootingBag = Query.inventory().nameEquals("Looting bag").findFirst().orElse(null);

            if (lootingBag != null) {
                Log.info(lootingBag.getId());
                if (lootingBag.getId() == 11941) {
                    lootingBag.click("Open");
                }
            }

            if (Prayer.getPrayerPoints() < 15) {
                PrayerManager.sipPrayer();
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

            if (MyPlayer.getCurrentHealthPercent() < 50) {
                Query.inventory().nameEquals("Shark").findClosestToMouse().map(InventoryItem::click);
                if (target != null){
                    target.click();
                }
                Waiting.wait(1500);
            }



            if (target == null){
                target = TargetManager.chooseNewTarget(TeleportManager.getMonsterIdBasedOnLocation(RevenantScript.selectedMonsterTile));
            }

            if (target != null) {
                var monster = Query.npcs().idEquals(TeleportManager.getMonsterIdBasedOnLocation(RevenantScript.selectedMonsterTile)).findRandom().orElse(null);
                if (monster != null){
                    if (monster.isInteractingWithMe() && !monster.isHealthBarVisible()){
                        monster.adjustCameraTo();
                        monster.click();
                    }
                }

                //Log.info("I have a target: " + target);
                if (!target.isVisible()){
                    GlobalWalking.walkTo(RevenantScript.selectedMonsterTile);
                }

                if (!target.isHealthBarVisible()){
                    target.click();
                    Waiting.waitUntil(500, () -> target.isHealthBarVisible());
                }


                if (target.isValid()){
                    if (target.getHealthBarPercent() != 0 && !target.isAnimating() && !target.isHealthBarVisible()) {
                        target.adjustCameraTo();
                        target.click();
                    }
                }

                if((target.getHealthBarPercent() == 0 && Query.npcs().idEquals(TeleportManager.getMonsterIdBasedOnLocation(RevenantScript.selectedMonsterTile)).isAny()) || !target.isHealthBarVisible() && target.isInteractingWithMe() || !target.isValid() || (target.isHealthBarVisible() && !target.isInteractingWithMe())){
                    target = TargetManager.chooseNewTarget(TeleportManager.getMonsterIdBasedOnLocation(RevenantScript.selectedMonsterTile));
                }



            }

                //Log.info("LOOT VALUE: " + LootingManager.getTripValue());
            if (LootingManager.getTripValue() >= 500000) {
                RevenantScript.state = State.BANKING;
            }

            // DO NOT HOP; KILL MOBS
        }else {
            if ((Query.players().isEquipped(22550, 12926).isAny() || Query.players().isBeingInteractedWith().isAny() || Query.players().isHealthBarVisible().isAny()) && !iWasFirst) {
                // Hop worlds

                WorldManager.hopToRandomMemberWorldWithRequirements();
            }
        }
    }

    public static boolean isiWasFirst() {
        return iWasFirst;
    }


    public static void setiWasFirst(boolean iWasFirst) {
        RevkillerManager.iWasFirst = iWasFirst;
    }
}
