package scripts;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.input.Mouse;
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
                Equipment.Slot.RING.getItem().map(c -> c.click("Grand Exchange"));
                RevenantScript.state = State.BANKING;
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
            CameraManager.setCameraAngle();

            if (Query.inventory().nameContains("Prayer potion").count() == 0) {
                Log.info("We are low on prayer. Teleporting out..");
                Equipment.Slot.RING.getItem().map(c -> c.click("Grand Exchange"));
                RevenantScript.state = State.BANKING;

                PrayerManager.disableQuickPrayer();
                return;
            }

            if (Query.inventory().nameEquals("Shark").count() < 4) {
                if (target.isValid()){
                    target.interact("Attack");
                    Waiting.waitUntil(() -> !target.isValid());
                    Waiting.wait(5000);
                    if (Query.groundItems().isAny() && LootingManager.hasLootBeenDetected()){
                        RevenantScript.state = State.LOOTING;
                    }
                }

                Log.info("We are low on shark. Teleporting out...");
                Equipment.Slot.RING.getItem().map(c -> c.click("Grand Exchange"));
                Log.debug("BANKING");
                RevenantScript.state = State.BANKING;
                PrayerManager.disableQuickPrayer();
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
                GameTab.EQUIPMENT.open();
                Waiting.wait(1500);
            }

            if (!BoostingManager.isBoosted()){
                BoostingManager.boost();
                if (target != null){
                    target.click();
                }
                GameTab.EQUIPMENT.open();
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
                GameTab.EQUIPMENT.open();
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


                if (!target.isVisible()){
                    GlobalWalking.walkTo(RevenantScript.selectedMonsterTile);
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

                if((target.getHealthBarPercent() == 0 && Query.npcs().idEquals(TeleportManager.getMonsterIdBasedOnLocation(RevenantScript.selectedMonsterTile)).isAny()) || !target.isValid() || (target.isHealthBarVisible() && !target.isInteractingWithMe())){
                    target = TargetManager.chooseNewTarget(TeleportManager.getMonsterIdBasedOnLocation(RevenantScript.selectedMonsterTile));
                }

            }
            if (Query.groundItems().isAny() && LootingManager.hasLootBeenDetected()){
                RevenantScript.state = State.LOOTING;
                return;
            }

            if (LootingManager.getTripValue() >= 500000) {
                Query.equipment().nameContains("Ring of wealth (").findFirst().map(ring -> ring.click("Grand exchange"));
                Log.info("Teleporting with: " + LootingManager.getTripValue());
                Waiting.waitUntil(10000,MyRevsClient::myPlayerIsInGE);
                if (MyRevsClient.myPlayerIsInGE()){
                    RevenantScript.state = State.BANKING;
                }
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
