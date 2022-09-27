package scripts.rev;

import dax.teleports.Teleport;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.input.Mouse;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Area;
import org.tribot.script.sdk.types.GameObject;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import org.tribot.script.sdk.walking.WalkState;
import scripts.api.MyBanker;
import scripts.api.MyClient;
import scripts.api.MyExchange;
import scripts.api.MyTeleporting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.tribot.script.sdk.Waiting.waitUntil;

public class TeleportManager {

    private static final WorldTile enclaveDoor = new WorldTile(3123, 3628, 0);
    private static final WorldTile caveEntrance = new WorldTile(3075, 3648, 0);
    private static final WorldTile enclavePool = new WorldTile(3137, 3629, 0);
    private static final WorldTile south_ork = new WorldTile(3216, 10094, 0);
    private static final WorldTile north_ork = new WorldTile(3226, 10132,0 );
    private static final WorldTile east_goblin = new WorldTile(3240, 10095, 0);
    private static final WorldTile demons = new WorldTile(3162, 10109,0 );
    private static boolean hasVisitedBeforeTrip = false;
    private static final List<WorldTile> monsterTiles = new ArrayList<>(Arrays.asList(demons, south_ork));// demons,  // South ork removed for now
    private final static Area FEROX_ENCLAVE = Area.fromRectangle(new WorldTile(3155, 3640, 0), new WorldTile(3116, 3623, 0));
    private final static Area SOUTH_ORK = Area.fromRectangle(new WorldTile(3200, 10105, 0), new WorldTile(3231, 10085, 0));
    private static GameObject pool = null;
    public static WorldTile refill() {
        // Random selection of mobs to kill



        var chosenMobArea = getRandomMobArea();

        if (!chosenMobArea.isVisible()) {
            BankManagerRevenant.drinkAntiVenom();
            if (Bank.isOpen()) {
                MyBanker.closeBank();
            }
            if (MyPlayer.getTile().getPlane() == 1 && !MyRevsClient.myPlayerIsInFerox()) {
                MyTeleporting.Dueling.FeroxEnclave.useTeleport();

            }

            if (MyRevsClient.myPlayerIsInWhitePortal()) {
                Query.gameObjects().idEquals(26646).findFirst().ifPresent(c -> c.click("Exit"));
            }

            Log.debug("[INFO_LISTENER] Started journey towards the cave...");

            if (MyRevsClient.myPlayerIsInCave()){
                Log.debug("i'm in cave. walking to mob area..");
                GlobalWalking.walkTo(chosenMobArea, () -> {

                    if (LootingManager.hasPkerBeenDetected()) {
                        MyRevsClient.getScript().setState(State.BANKING);
                        return WalkState.FAILURE;
                    }
                    if (!MyRevsClient.myPlayerIsInCave()) {
                        refill();
                        return WalkState.SUCCESS;
                    }
                    return WalkState.CONTINUE;
                });
                chosenMobArea.clickOnMinimap();
            }
//
            if (MyRevsClient.myPlayerIsInGE() || MyRevsClient.myPlayerIsInCasteWars() || MyRevsClient.myPlayerIsAtEdge() || MyPlayer.getTile().getPlane() == 2){
                MyBanker.closeBank();
                if (!MyTeleporting.Dueling.FeroxEnclave.useTeleport()) {
                        if (!Query.inventory().nameContains("Ring of dueling(").isAny()){
                            BankManagerRevenant.withdrawFoodAndPots();
                        }
                        Log.debug("Couldn't teleport to ferox.. You must be missing a ring of dueling");
                    }
                Waiting.waitNormal(1200, 200);
            }

            if (MyRevsClient.myPlayerIsInFerox()) {
                setHasVisitedBeforeTrip(true);
                Log.debug("[INFO_LISTENER] I'm in ferox");
                if (!BankManagerRevenant.isEquipmentBankTaskSatisfied()){
                    MyBanker.openBank();
                    MyBanker.depositInventory();
                    BankManagerRevenant.equipAndChargeItems();
                    BankManagerRevenant.getEquipmentBankTask().execute();
                    BankManagerRevenant.wearAvarice();
                }

                if (Inventory.getAll().size() <= 20){
                    MyBanker.openBank();
                    BankManagerRevenant.getInventoryBankTask().execute();
                    BankManagerRevenant.emptyLootingBag();
                    MyBanker.closeBank();
                }

                if (MyRevsClient.myPlayerNeedsToRefresh()){
                    Log.debug("I'm walking to pool");
                    GlobalWalking.walkTo(enclavePool, () -> {
                        if (LootingManager.hasPkerBeenDetected()) {
                            MyRevsClient.getScript().setState(State.BANKING);
                            return WalkState.FAILURE;
                        }

                        return WalkState.CONTINUE;
                    });

                    Query.gameObjects().idEquals(39651).findClosest().map(c -> {
                        if (c.interact("Drink")) {
                            Log.debug("I'm trying to drink from the pool");
                            waitUntil(() -> MyPlayer.getAnimation() == 7305);
                            Waiting.waitUntil(() -> MyPlayer.getAnimation() == -1);
                            Waiting.waitNormal(1000, 200);
                            return true;
                        }
                        return false;
                    });
                }else {
                    Log.debug("I'm walking to entrance");
                    GlobalWalking.walkTo(caveEntrance, () ->{
                        if (LootingManager.hasPkerBeenDetected()) {
                            MyRevsClient.getScript().setState(State.BANKING);
                            return WalkState.FAILURE;
                        }

                        return WalkState.CONTINUE;
                    });
                }
                }
            }

            if (!MyRevsClient.myPlayerIsInCave()){
                if (Query.gameObjects().idEquals(31555).findBestInteractable().isPresent()){
                    Log.debug("I'm entering cave");
                    Query.gameObjects().idEquals(31555).findBestInteractable().ifPresent(c -> c.interact("Enter"));
                    if (Waiting.waitUntil(500, () -> MyClient.isWidgetVisible(193, 0, 2))) {
                        Waiting.waitUntil( () -> MyClient.clickWidget("Continue", 193, 0, 2));
                    }
                    if (ChatScreen.isOpen()){
                        Waiting.waitNormal(1000,100);
                        Waiting.waitUntil(ChatScreen::clickContinue);
                        Waiting.waitUntil(() -> ChatScreen.containsOption("Yes, don't ask again."));
                        Waiting.waitUntil((() -> ChatScreen.selectOption("Yes, don't ask again.")));

                    }
                    Log.debug("Waiting to be in cave");
                    Waiting.waitUntil(3000, MyRevsClient::myPlayerIsInCave);
                    Log.debug("Am i in cave? " + MyRevsClient.myPlayerIsInCave());
                }
            }

        if (Query.npcs().nameEquals("Death").isAny()){
            Query.gameObjects().idEquals(39549).findFirst().ifPresent(portal -> portal.click("Use"));
        }


        Mouse.setSpeed(197);
        return chosenMobArea;
    }

    private static WorldTile getRandomMobArea() {
        Collections.shuffle(monsterTiles);
        return monsterTiles.get(0);
    }

    private static void setWalkingState(){

/*
        Mouse.setSpeed(700);
        MyOptions.setRunOn();
        MyCamera.init();

        if (!GameTab.LOGOUT.isOpen()) {
            GameTab.LOGOUT.open();
        }

        if (MyClient.isWidgetVisible(182, 3)) {
            MyClient.clickWidget("World Switcher", 182, 3);
        }*/

        /*if (Query.players()
                .withinCombatLevels(Combat.getWildernessLevel())
                .isNotEquipped(DetectPlayerThread.getPvmGear())
                .notInArea(FEROX_ENCLAVE)
                .findFirst().isPresent()){
            WorldManager.hopToRandomMemberWorldWithRequirements();
        }*/

    }

    public static boolean monsterTileIsDetected(WorldTile tile){
        return tile.isRendered() || tile.isVisible();
    }

    public static void teleportOutOfWilderness(String message){
        Log.debug("[WILDERNESS_LISTENER] " + message);
        // teleport out
        if (!MyExchange.walkToGrandExchange()){
            Log.debug("[WILDERNESS_LISTENER] Couldn't teleport out of wilderness. You must be missing wealth or be above 30 wilderness");

        }
        var isInGe = Waiting.waitUntil(MyRevsClient::myPlayerIsInGE);
        if (isInGe){
            Log.debug("[WILDERNESS_LISTENER] Teleported out to safety. We are in GE");
            PrayerManager.disableQuickPrayer();
        }else {
            Log.debug("Couldn't teleport out of wilderness. Trying again...");
            MyExchange.walkToGrandExchange();
            teleportOutOfWilderness(message);
        }
        MyRevsClient.getScript().setState(scripts.rev.State.BANKING);

    }

    public static boolean teleportToGE(){
        if (Teleport.RING_OF_WEALTH_GRAND_EXCHANGE.getRequirement().satisfies()) {
            // can teleport
            return Teleport.RING_OF_WEALTH_GRAND_EXCHANGE.trigger();
        }
        return false;
    }



    public static boolean isHasVisitedBeforeTrip() {
        return hasVisitedBeforeTrip;
    }

    public static void setHasVisitedBeforeTrip(boolean hasVisitedBeforeTrip) {
        TeleportManager.hasVisitedBeforeTrip = hasVisitedBeforeTrip;
    }

    public static WorldTile getDemons() {
        return demons;
    }

    public static WorldTile getSouth_ork() {
        return south_ork;
    }

    public static int getMonsterIdBasedOnLocation(WorldTile tile){
        if (tile.getX() == 3240) {
            return 7933;
        }
        if (tile.getX() != demons.getX()){
            return 7937;
        }
            return 7936;
    }

    public static void teleportOut() {

        if (!Combat.isInWilderness()) {
            Log.debug("not in wildy. Stopping teleport process");
            return;
        }
        Log.debug("Teleport out process has begun");
        if (MyRevsClient.myPlayerIsInCave()) {
            if (MyRevsClient.getScript().getSelectedMonsterTile().getX() == TeleportManager.getSouth_ork().getX()){
                var location = new WorldTile(3205, 10082, 0);
            GlobalWalking.walkTo(location, () -> {
                if ((LootingManager.hasPkerBeenDetected() && !Combat.isInWilderness()) || location.isOnMinimap() || !Combat.isInWilderness()) {
                    Log.debug("Failure..");
                    return WalkState.FAILURE;
                }
                return WalkState.CONTINUE;
            });
        }else if(MyRevsClient.getScript().getSelectedMonsterTile().getX() == TeleportManager.getDemons().getX()) {
                var location = new WorldTile(3145, 10110);
                GlobalWalking.walkTo(location, () -> {
                    if ((LootingManager.hasPkerBeenDetected() && !Combat.isInWilderness()) || location.isOnMinimap() || !Combat.isInWilderness()) {
                        Log.debug("Failure..");
                        return WalkState.FAILURE;
                    }
                    return WalkState.CONTINUE;
                });
            }
    }

        Waiting.wait(2500);
        if (Query.inventory().nameContains("Ring of wealth (").isAny() && !Query.equipment().nameContains("Ring of wealth (").isAny()) {
            Query.inventory().nameContains("Ring of wealth (").findClosestToMouse().map(c -> c.click("Wear"));
            Waiting.waitUntil(() -> Query.equipment().nameContains("Ring of wealth (").isAny());
            Equipment.Slot.RING.getItem().ifPresent(ring -> ring.click("Grand Exchange"));
        }

        if (Query.equipment().nameContains("Ring of wealth (").isAny()) {
            Equipment.Slot.RING.getItem().ifPresent(ring -> ring.click("Grand Exchange"));
        }

        var inGe = Waiting.waitUntil(5000, MyRevsClient::myPlayerIsInGE);
        if (!inGe) {
            Log.debug("Not in GE. trying again....");
            Equipment.Slot.RING.getItem().ifPresent(c -> c.click("Grand Exchange"));
        }
        Log.debug("Switching to banking state");
        MyRevsClient.getScript().setState(State.BANKING);
        Waiting.waitNormal(1500, 100);
    }

    public static Area getSouthOrk() {
        return SOUTH_ORK;
    }

    /*
    public static WalkState processWalking(){


        return WalkState.CONTINUE;
    }*/
}
