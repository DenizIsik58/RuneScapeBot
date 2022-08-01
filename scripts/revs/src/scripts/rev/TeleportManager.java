package scripts.rev;

import dax.teleports.Teleport;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
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
    private static final WorldTile enclavePool = new WorldTile(3128, 3634, 0);
    private static final WorldTile south_ork = new WorldTile(3216, 10091, 0);
    private static final WorldTile north_ork = new WorldTile(3226, 10132,0 );
    public static final WorldTile demons = new WorldTile(3160, 10115,0 );
    private static boolean hasVisitedBeforeTrip = false;

    public static WorldTile refill() {
        List<WorldTile> monsterTiles = new ArrayList<>(Arrays.asList(south_ork, north_ork, demons));
        Collections.shuffle(monsterTiles);
        var chosenMobArea = monsterTiles.get(0);
        Waiting.waitUntil(10000, MyRevsClient::myPlayerIsInFerox);

        if (!chosenMobArea.isVisible()) {
            Log.debug("[INFO_LISTENER] Started journey towards the cave...");
            if (Combat.getWildernessLevel() > 20){
                GlobalWalking.walkTo(enclaveDoor);
            }

            if (!MyRevsClient.myPlayerIsInFerox() && !isHasVisitedBeforeTrip()){
                Log.debug("[INFO_LISTENER] Trying teleporting to ferox!");
                if (!MyTeleporting.Dueling.FeroxEnclave.useTeleport()){
                    Log.debug("Couldn't teleport to ferox.. You must be missing a ring of dueling or be above 20 wilderness");
                }
                    setHasVisitedBeforeTrip(true);
            }

            if (MyRevsClient.myPlayerIsInFerox()) {
                setHasVisitedBeforeTrip(true);
                Log.debug("[INFO_LISTENER] I'm in ferox");
                if (MyRevsClient.myPlayerNeedsToRefresh()){
                    Log.debug("I'm walking to pool");
                    GlobalWalking.walkTo(enclavePool);

                    Query.gameObjects().idEquals(39651).findClosest().map(c -> c.interact("Drink"));
                    Log.debug("I'm trying to drink from the pool");
                    waitUntil(() -> MyPlayer.getAnimation() == 7305);
                    Waiting.wait(3000);
                }else {
                    Log.debug("I'm walking to entrance");
                    GlobalWalking.walkTo(caveEntrance);

                }
            }

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
            }

            if (MyRevsClient.myPlayerIsInCave()){
                Log.debug("i'm in cave. walking to mob area..");
                GlobalWalking.walkTo(chosenMobArea);
            }

        }
        return chosenMobArea;
    }

    public static boolean monsterTileIsDetected(WorldTile tile){
        return tile.isRendered() || tile.isVisible() || tile.isInLineOfSight();
    }

    public static void teleportOutOfWilderness(String message){
        Log.debug("[WILDERNESS_LISTENER] " + message);
        // teleport out
        if (!MyExchange.walkToGrandExchange()){
            Log.debug("[WILDERNESS_LISTENER] Couldn't teleport out of wilderness. You must be missing wealth or be above 30 wilderness");

        }
        Log.debug("[WILDERNESS_LISTENER] Teleported out to safety. We are in GE");
        PrayerManager.disableQuickPrayer();
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
/*
    public static WalkState processWalking(){


        return WalkState.CONTINUE;
    }*/
}
