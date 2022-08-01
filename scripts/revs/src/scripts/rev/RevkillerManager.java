package scripts.rev;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.types.Npc;
import org.tribot.script.sdk.walking.GlobalWalking;
import scripts.api.MyCamera;

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

                // teleport out
                TeleportManager.teleportToGE();
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
            PrayerManager.enableQuickPrayer();
            MyCamera.setCameraAngle();

            if (Query.inventory().nameContains("Prayer potion").count() == 0) {
                Log.info("We are low on prayer. Teleporting out..");
                TeleportManager.teleportToGE();

                return;
            }

            if (Query.inventory().nameEquals("Shark").count() < 4) {
                if (target != null){
                    if (target.isValid()){
                        target.interact("Attack");
                        Waiting.waitUntil(() -> !target.isValid());
                        Waiting.waitNormal(5000, 500);
                        if (Query.groundItems().isAny() && LootingManager.hasLootBeenDetected()){
                            MyRevsClient.getScript().setState(State.LOOTING);
                        }
                    }
                }

                Log.info("We are low on shark. Teleporting out...");
                TeleportManager.teleportToGE();
                Log.debug("BANKING");
                return;
            }

            var lootingBag = Query.inventory().nameEquals("Looting bag").findFirst().orElse(null);

            if (lootingBag != null) {
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

            if (MyPlayer.getCurrentHealthPercent() < 70) {
                Query.inventory().nameEquals("Shark").findClosestToMouse().map(InventoryItem::click);
                if (target != null){
                    target.click();
                }
                Waiting.wait(1500);
            }



            if (target == null){
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
                    target = TargetManager.chooseNewTarget(TeleportManager.getMonsterIdBasedOnLocation(MyRevsClient.getScript().getSelectedMonsterTile()));
                }

            }
            if (Query.groundItems().isAny() && LootingManager.hasLootBeenDetected()){
                MyRevsClient.getScript().setState(State.LOOTING);
                return;
            }

            if (LootingManager.getTripValue() >= 500000) {
                TeleportManager.teleportToGE();
            }

            // DO NOT HOP; KILL MOBS
        }else {
            if ((Query.players().isEquipped(22550, 12926).isAny() || Query.players().isBeingInteractedWith().isAny() || Query.players().isHealthBarVisible().isAny()) && !iWasFirst) {
                // Hop worlds
                WorldManager.hopToRandomMemberWorldWithRequirements();
            }
        }
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
