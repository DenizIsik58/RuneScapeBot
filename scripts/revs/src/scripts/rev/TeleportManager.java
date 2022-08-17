package scripts.rev;

import dax.teleports.Teleport;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.input.Mouse;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Area;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import org.tribot.script.sdk.walking.WalkState;
import scripts.api.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.tribot.script.sdk.Waiting.waitUntil;

public class TeleportManager {

    private static final WorldTile enclaveDoor = new WorldTile(3123, 3628, 0);
    private static final WorldTile caveEntrance = new WorldTile(3075, 3648, 0);
    private static final WorldTile enclavePool = new WorldTile(3128, 3634, 0);
    private static final WorldTile south_ork = new WorldTile(3216, 10091, 0);
    private static final WorldTile north_ork = new WorldTile(3226, 10132,0 );
    public static final WorldTile demons = new WorldTile(3160, 10115,0 );
    private static boolean hasVisitedBeforeTrip = false;
    private static List<WorldTile> monsterTiles = new ArrayList<>(Collections.singletonList(demons)); // South ork removed for now
    private final static Area FEROX_ENCLAVE = Area.fromRectangle(new WorldTile(3155, 3640, 0), new WorldTile(3116, 3623, 0));

    public static WorldTile refill() {
        // Random selection of mobs to kill

        var chosenMobArea = south_ork; //getRandomMobArea();

        if (!chosenMobArea.isVisible()) {
            if (MyRevsClient.myPlayerIsInWhitePortal()) {
                Query.gameObjects().idEquals(26646).findFirst().ifPresent(c -> c.click("Exit"));
            }
            Log.debug("[INFO_LISTENER] Started journey towards the cave...");

            if (GameState.isLoading()) {
                Log.debug("Game is loading");
                Waiting.waitUntil(10000, () -> !GameState.isLoading());
            }

            if (MyRevsClient.myPlayerIsInCave()){
                Log.debug("i'm in cave. walking to mob area..");
                GlobalWalking.walkTo(south_ork, () -> {
                    if (!MyRevsClient.myPlayerIsInCave()) {
                        refill();
                        return WalkState.SUCCESS;
                    }
                    setWalkingState();
                    return WalkState.CONTINUE;
                });
            }

            if (MyRevsClient.myPlayerIsInGE() || MyRevsClient.myPlayerIsInCasteWars()){
                    if (!MyTeleporting.Dueling.FeroxEnclave.useTeleport()) {
                        if (!Query.inventory().nameContains("Ring of dueling(").isAny()){
                            BankManagerRevenant.withdrawFoodAndPots();
                        }
                        Log.debug("Couldn't teleport to ferox.. You must be missing a ring of dueling");
                    }
            }

            if (MyRevsClient.myPlayerIsInFerox()) {
                setHasVisitedBeforeTrip(true);
                Log.debug("[INFO_LISTENER] I'm in ferox");
                if (MyRevsClient.myPlayerNeedsToRefresh()){
                    Log.debug("I'm walking to pool");
                    GlobalWalking.walkTo(enclavePool, () -> {
                        setWalkingState();
                        return WalkState.CONTINUE;
                    });

                    Query.gameObjects().idEquals(39651).findClosest().map(c -> c.interact("Drink"));
                    Log.debug("I'm trying to drink from the pool");
                    waitUntil(() -> MyPlayer.getAnimation() == 7305);
                    Waiting.wait(2000);
                }else {
                    Log.debug("I'm walking to entrance");
                    GlobalWalking.walkTo(caveEntrance, () ->{
                        setWalkingState();
                        return WalkState.CONTINUE;
                    });
                }

                }
                if (!BankManagerRevenant.isEquipmentBankTaskSatisfied()){
                    MyBanker.openBank();
                    BankManagerRevenant.getEquipmentBankTask().execute();
                }

                if (Inventory.getAll().size() <= 20){
                    MyBanker.openBank();
                    BankManagerRevenant.getInventoryBankTask().execute();
                }
            }

            if (!MyRevsClient.myPlayerIsInCave()){
                if (Query.gameObjects().idEquals(31555).findBestInteractable().isPresent()){
                    Log.debug("I'm entering cave");
                    Query.gameObjects().idEquals(31555).findBestInteractable().map(c -> c.interact("Enter"));
                    if (ChatScreen.isOpen()){
                        Waiting.waitUntil(ChatScreen::clickContinue);
                        Waiting.waitUntil(() -> ChatScreen.containsOption("Yes, don't ask again."));
                        Waiting.waitUntil((() -> ChatScreen.selectOption("Yes, don't ask again.")));

                    }
                    Log.debug("Waiting to be in cave");
                    Waiting.waitUntil(5000, MyRevsClient::myPlayerIsInCave);
                    Log.debug("Am i in cave? " + MyRevsClient.myPlayerIsInCave());
                    Waiting.waitNormal(2000, 200);
                }
            }

        Mouse.setSpeed(300);
        return south_ork;
    }

    private static WorldTile getRandomMobArea() {
        Collections.shuffle(monsterTiles);
        return monsterTiles.get(0);
    }

    private static void setWalkingState(){
        Mouse.setSpeed(700);
        MyOptions.setRunOn();
        MyCamera.init();

        if (!GameTab.LOGOUT.isOpen()) {
            GameTab.LOGOUT.open();
        }

        if (MyClient.isWidgetVisible(182, 3)) {
            MyClient.clickWidget("World Switcher", 182, 3);
        }

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

    /* public static WorldTile refillAndWalkToCave(){

                if (Equipment.getAll().size() != 9){
                    BankManagerRevenant.withdrawGear();
                }

                if (Combat.getWildernessLevel() > 20){
                    GlobalWalking.walkTo(enclaveDoor);
                }

                var ring = Query.inventory().nameContains("Ring of dueling").findFirst().orElse(null);
                if (ring != null){
                    if (!MyRevsClient.myPlayerIsInFerox()) {
                        ring.click("Rub");
                        Waiting.waitUntil(2000, () -> ChatScreen.containsOption("Ferox Enclave."));
                        ChatScreen.selectOption("Ferox enclave.");
                        Waiting.waitUntil(MyRevsClient::myPlayerIsInFerox);
                    }
                }else {
                        BankManagerRevenant.withdrawItemByName("Ring of dueling");
                        Bank.close();
                        Waiting.wait(4000);
                }
                List<WorldTile> monsterTiles = new ArrayList<>(Arrays.asList(south_ork, north_ork, demons));
                Collections.shuffle(monsterTiles);
                var randomMonsterTile = monsterTiles.get(0);

                if (MyRevsClient.myPlayerIsInFerox()) {
                    GlobalWalking.walkTo(enclavePool);
                    var pool = Query.gameObjects().idEquals(39651).findClosest().orElse(null);
                    if (pool != null) {
                        if (pool.isVisible()) {
                            pool.interact("Drink");
                            waitUntil(() -> MyPlayer.getAnimation() == 7305);
                            Waiting.wait(3000);
                        }
                    }

                    if (!MyPlayer.isStaminaActive()) {
                        Query.inventory().nameContains("Stamina potion").findFirst().map(InventoryItem::click);
                    }

                    if (!Options.isRunEnabled()){
                        Options.setRunEnabled(true);
                    }

                    GlobalWalking.walkTo(caveEntrance);
                    // Cave entrance
                    Query.gameObjects().idEquals(31555).findBestInteractable().map(c -> c.interact("Enter"));
                    if (ChatScreen.isOpen()){
                        Waiting.waitUntil(ChatScreen::clickContinue);
                        Waiting.waitUntil(() -> ChatScreen.containsOption("Yes, don't ask again."));
                        Waiting.waitUntil((() -> ChatScreen.selectOption("Yes, don't ask again.")));

                    }
                    Waiting.waitUntilInArea(Area.fromRectangle(new WorldTile(3192, 10062, 0), new WorldTile(3202, 10051, 0)), 10000);

                    GlobalWalking.walkTo(randomMonsterTile);

                }
                return randomMonsterTile;
            }
        */
    public static int getMonsterIdBasedOnLocation(WorldTile tile){
        if (tile.getX() != demons.getX()){
            return 7937;
        }
            return 7936;
    }

    public static void teleportOut() {
        var location = new WorldTile(3205, 10082, 0);
        GlobalWalking.walkTo(location,  () -> {
            if ((LootingManager.hasPkerBeenDetected() && !Combat.isInWilderness()) || location.isOnMinimap()) {
                return WalkState.FAILURE;
            }
            return WalkState.CONTINUE;
        });

        Waiting.wait(2000);
        Equipment.Slot.RING.getItem().ifPresent(c -> c.click("Grand Exchange"));
        MyRevsClient.getScript().setState(State.BANKING);
    }
/*
    public static WalkState processWalking(){


        return WalkState.CONTINUE;
    }*/
}
