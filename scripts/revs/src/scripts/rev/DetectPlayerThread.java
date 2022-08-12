package scripts.rev;


import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.Setter;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.input.Mouse;
import org.tribot.script.sdk.interfaces.Item;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Area;
import org.tribot.script.sdk.types.Player;
import org.tribot.script.sdk.types.Projectile;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import org.tribot.script.sdk.walking.WalkState;
import scripts.api.*;
import scripts.api.utility.StringsUtility;

import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.tribot.script.sdk.Combat.getWildernessLevel;


public class DetectPlayerThread extends Thread {


    @Setter
    private static int tickCounter = 0;
    private final RevScript script;
    private final AtomicBoolean teleblocked = new AtomicBoolean(false);
    private final AtomicBoolean danger = new AtomicBoolean(false);
    //    private final AtomicBoolean dangerFlag = new AtomicBoolean(false);
    private final AtomicBoolean isAntiPking = new AtomicBoolean(false);
    private final static String[] PVM_GEAR = new String[]{"Black d'hide body","Toxic blowpipe", "Magic shortbow", "Magic shortbow (i)", "Craw's bow", "Viggora's chainmace"};
    private final static Area FEROX_ENCLAVE = Area.fromRectangle(new WorldTile(3155, 3640, 0), new WorldTile(3116, 3623, 0));
    private final SimpleBooleanProperty running = new SimpleBooleanProperty(false);
    private final AtomicBoolean hasPkerBeenDetected = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private static boolean hasTickCounterStarted = false;
    private static final AtomicBoolean outOfFood = new AtomicBoolean(false);

    @Getter @Setter
    private static Projectile lastProjectile = null;
    @Getter @Setter
    private static boolean isEntangleTimerStarted = false;
    private static boolean isEntangled = false;
    private static MagicManager entangleDetecter = null;

    public DetectPlayerThread(RevScript revScript) {
        this.script = revScript;
        ScriptListening.addPauseListener(() -> paused.set(true));
        ScriptListening.addResumeListener(() -> paused.set(false));

    }

    private void handleTeleblock() {
        var lastTeleblockNotification = MyScriptVariables.getVariable("lastTeleblockNotification", 0L);

        if (MyRevsClient.myPlayerIsDead() || MyRevsClient.myPlayerIsInGE() || MyRevsClient.myPlayerIsInFerox()){
            MyScriptVariables.setVariable("lastTeleblockNotification", 0L);
            lastTeleblockNotification = MyScriptVariables.getVariable("lastTeleblockNotification", 0L);

        }
        // 5 mins might be too long and run into going another trip after?
        if (System.currentTimeMillis() - lastTeleblockNotification < (60 * 1000) * 5) {
            Log.debug("Handling teleblock");
            setTeleblocked(true);
        } else setTeleblocked(false);

    }


    public void stopDetection() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public static boolean detectPKers() {

        return Query.players()
                .withinCombatLevels(getWildernessLevel())
                .notInArea(FEROX_ENCLAVE)
                .isNotEquipped(PVM_GEAR)
                .isAny();
    }

    public static boolean detectSkull() {
        return Query.players()
                .withinCombatLevels(getWildernessLevel())
                .notInArea(FEROX_ENCLAVE)
                .isNotEquipped(PVM_GEAR)
                .hasSkullIcon()
                .isAny();

    }

    public static boolean detectRaggers() {
        return Query.players()
                .withinCombatLevels(getWildernessLevel())
                .hasSkullIcon()
                .notInArea(FEROX_ENCLAVE)
                .isInteractingWithMe()
                .isAny();
    }

    public static boolean canTargetAttackMe(String name){
        return Query.players()
                .withinCombatLevels(getWildernessLevel())
                .nameEquals(name)
                .notInArea(FEROX_ENCLAVE)
                .isAny();
    }

    public boolean isAntiPking() {
        return isAntiPking.get();
    }

    public void setAntiPking(boolean value) {
        isAntiPking.set(value);
    }

    public boolean inDanger() {
        return danger.get();
    }

    public void setInDanger(boolean value) {
        danger.set(value);
    }

    public boolean isTeleblocked() {
        return teleblocked.get();
    }

    public void setTeleblocked(boolean value) {
        teleblocked.set(value);
    }

    public void setHasPkerBeenDetected(boolean hasPkerBeenDetected) {
        //this will cancel our walking on player detected
        script.cancelWalking();
        // and every time that condition cancels a walk it resets the time, so
        // it will remain cancelled until our set amount of time after the last time it cancels
        this.hasPkerBeenDetected.set(hasPkerBeenDetected);
    }

    public boolean hasPkerBeenDetected() {
        return hasPkerBeenDetected.get();
    }

    public static Player getPker() {
        var name = getPkerName();
        if (name.isEmpty()) return null;
        return Query.players()
                .filter(player -> StringsUtility.runescapeStringsMatch(player.getName(), name))
                .findFirst()
                .orElse(null);
    }

    public static String getPkerName() {
        return MyScriptVariables.getVariable("pkerName", "");
    }

    public static void resetFreezeTimer() {
             new java.util.Timer().schedule(new TimerTask() {
                 @Override
                 public void run() {
                     Log.debug("Timer is over we are unfrozen!");
                     lastProjectile = null;
                     isEntangleTimerStarted = false;
                     isEntangled = false;
                 }
             }, 15000);
    }

    public static void handleEatAndPrayer(Player pker) {

        pker.getEquippedItem(Equipment.Slot.WEAPON).map(Item::getName).ifPresent(playerWeapon -> {
            if (playerWeapon.toLowerCase().contains("staff")) {
                // Magic weapon
                // 1. Set up prayer according to weapon
                PrayerManager.enablePrayer(Prayer.PROTECT_FROM_MAGIC);

            } else if (playerWeapon.toLowerCase().contains("dragon") || playerWeapon.toLowerCase().contains("maul") || playerWeapon.toLowerCase().contains("scimitar")) {
                // Handle melee weapon
                // 1. Set up prayer according to weapon
                PrayerManager.enablePrayer(Prayer.PROTECT_FROM_MELEE);
            } else /*if (playerWeapon.toLowerCase().contains("crossbow"))*/ {
                // Handle ranging weapon
                // 1. Set up prayer according to weapon
                PrayerManager.enablePrayer(Prayer.PROTECT_FROM_MISSILES);
            }
        });

        if (MyAntiBan.shouldEat()) {
            var sharkCount = Inventory.getCount("Shark");
            if (sharkCount > 0) {
                var ate = Query.inventory()
                        .nameEquals("Shark")
                        .findClosestToMouse()
                        .map(c -> c.click("Eat")
                                    && Waiting.waitUntil(1000, () -> Inventory.getCount("Shark") < sharkCount))
                        .orElse(false);
                if (ate) MyAntiBan.calculateNextEatPercent();
            } else {
                outOfFood.set(true);
                Log.warn("Out of food under eat percent");
            }
        }
        if (MyPrayer.shouldDrinkPrayerPotion()) {
            PrayerManager.maintainPrayerPotion();
        }
    }

    public void antiPk() {
        var pker = getPker();

        while (isTeleblocked()) {
            if (entangleDetecter == null) {
                entangleDetecter = new MagicManager();
                new Thread(entangleDetecter).start();
            }

            if (pker != null) {
                if (!canTargetAttackMe(pker.getName())) {
                    // run away if our target is not nearby
                    Log.debug("trying to hop worlds... Target is not in sight");
                    WorldManager.hopToRandomMemberWorldWithRequirements();
                    TeleportManager.teleportOutOfWilderness("We are trying to teleport out. Target not in sight");
                    return;
                }

                handleEatAndPrayer(pker);

                if (!isFrozen()) {
                    // Start running
                    Log.debug("Im not frozen. Running!");

                    if (MyRevsClient.myPlayerIsInCave()) {
                        WorldTile stairs = new WorldTile(3217, 10058, 0); // Tile to climb up at

                        GlobalWalking.walkTo(stairs, () -> {
                            handleEatAndPrayer(pker);
                            Log.debug("Returning failure");
                            return WalkState.FAILURE;
                        });
                        Query.gameObjects().idEquals(31558).findBestInteractable()
                                .map(c -> c.interact("Climb-up")
                                        && Waiting.waitUntil(500, () -> !MyRevsClient.myPlayerIsInCave()))
                                .orElse(false);
                        handleEatAndPrayer(pker);

                    } else {

                        //handleEatAndPrayer(pker);
                        ensureWalkingPermission();
                        //MyExchange.walkToGrandExchange();
                        MyPlayer.getTile().translate(0, -15).clickOnMinimap();
                        handleEatAndPrayer(pker);
                    }
                    continue;
                }
            }

            Waiting.wait(100);
        }

    }

    public static Optional<Projectile> getProjectile(){
        return Query.projectiles()
                .isTargetingMe()
                .isMoving()
                .graphicIdEquals(178)
                .findFirst();
    }

    public static void proj(){
        getProjectile().ifPresent(prj -> Log.debug("PROJECTILE: " + prj.getGraphicId()));
        getProjectile().ifPresent(p -> Log.debug("START: " + p.getStart()));
        getProjectile().ifPresent(p -> Log.debug("DESTINATION: " + p.getDestination()));
        Log.debug("MY tile: " + MyPlayer.getTile());
    }

    public static void setProjectile(){
        getProjectile().ifPresent(proj -> {
            Log.debug("Setting projectile!");
            lastProjectile = proj;
        });
    }

    public static boolean isFrozen(){
        if (isEntangled) {
            return true;
        }

        if (lastProjectile != null && !isEntangleTimerStarted) {
            Log.debug("Timer is not started and last entangle is not null");
            var isFrozen =  lastProjectile.getDestination().equals(MyPlayer.getTile()) && !MyPlayer.isMoving();
            if (isFrozen) {
                Log.debug("Entangle successfully landed");
                Log.debug("Our player is not moving. We are frozen");
                isEntangleTimerStarted = true;
                isEntangled = true;
                resetFreezeTimer();
                return true;
            }
        }
        return false;
    }

    private void ensureWalkingPermission() {
        if (script.isCancellingWalking()) {
            // i have the timer set for cancelling walking for 1 full second currently to test
            // so going to give it time for 5 cancels which should be insanely more than enough...
            // after successful testing we can decrease these times to like 250 ms wait and 500
            // TODO: Decrease the time to wait before walking (here and the debounce) after testing.
            Waiting.waitUntil(5000, () -> {
                // this is going to spam xD testing only lol
                Log.trace("Pker thread is waiting for permission to take over walking");
                return !script.isCancellingWalking();
            });
        }
    }

    @Override
    public void run() {
        running.set(true);
        while (running.get()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Log.debug(e);
                e.printStackTrace();
            }
            if (paused.get()) continue;

            var danger = inDanger();
            var detectedPkers = detectPKers();
            var detectedRaggers = detectRaggers();
            var detectedSkull = detectSkull();
            var teleblocked = isTeleblocked();


            handleTeleblock();

            if (Combat.isInWilderness()) {
                if (!danger) {

                    if (detectedPkers || detectedRaggers || detectedSkull || teleblocked) {
                        if (detectedPkers) Log.warn("[DANGER_LISTENER] DETECTED DANGER - PKER");
                        if (detectedRaggers) Log.warn("[DANGER_LISTENER] DETECTED DANGER - RAGGER");
                        if (detectedSkull) Log.warn("[DANGER_LISTENER] DETECTED DANGER - SKULLED PLAYER");
                        if (teleblocked) Log.warn("[DANGER_LISTENER] DETECTED DANGER - TELEBLOCK");

                        setInDanger(true);
                        setHasPkerBeenDetected(true);
                    }
                    continue;
                }

                danger = inDanger();
                if (danger) {
                    Log.warn("[DANGER_LISTENER] HANDLING DANGER");
                    if (Mouse.getSpeed() == 200) {
                        int dangerMouseSpeed = getRandomNumber(1500, 2000);
                        Mouse.setSpeed(dangerMouseSpeed);
                    }
                    teleblocked = isTeleblocked();
                    if (!teleblocked) {
                        Log.trace("[DANGER_LISTENER] NOT TELEBLOCKED - Teleporting");
                        if (isAntiPking()) {
                            setAntiPking(false);
                        }
                        Query.players()
                                .withinCombatLevels(Combat.getWildernessLevel())
                                .isNotEquipped(PVM_GEAR)
                                .findFirst()
                                .ifPresent(pker -> {
                                   /* if (pker.getTile().getY() > MyPlayer.getTile().getY()){
                                        // Player is more north than me
                                        // Run south
                                        MyPlayer.getTile().translate(0, -10).clickOnMinimap();
                                    }else {
                                        // Run north
                                    }*/
                                            Log.debug("Timer has started: " + hasTickCounterStarted());

                                            if (pker.getTile().getX() > MyPlayer.getTile().getX()) {
                                                // Player is east
                                                // Run west
                                                Log.debug("Player on east. Running west!");
                                                MyPlayer.getTile().translate(-15, 0).clickOnMinimap();
                                                if (!hasTickCounterStarted){
                                                    Log.debug("Timer for teleport has beenn started");
                                                    hasTickCounterStarted = true;
                                                    new java.util.Timer().schedule(new TimerTask() {
                                                        @Override
                                                        public void run() {
                                                            if (hasTickCounterStarted){
                                                                Log.debug("1,8 seconds gone Teleporting now");
                                                                    Equipment.Slot.RING.getItem().map(c -> c.click("Grand Exchange"));
                                                                }


                                                        }
                                                    }, 1800);
                                                }

                                            } else {
                                                //Player west
                                                // Run east
                                                Log.debug("Player on west. Running east!");
                                                MyPlayer.getTile().translate(15, 0).clickOnMinimap();
                                                if (!hasTickCounterStarted){
                                                    Log.debug("Timer for teleport has beenn started");
                                                    hasTickCounterStarted = true;
                                                    new java.util.Timer().schedule(new TimerTask() {
                                                        @Override
                                                        public void run() {
                                                            if (hasTickCounterStarted){
                                                                Log.debug("1,8 seconds gone Teleporting now");
                                                                Equipment.Slot.RING.getItem().map(c -> c.click("Grand Exchange"));
                                                            }
                                                        }
                                                    }, 1800);
                                                }

                                            }
                                        });




                                    /*if (tickCounter() >= 3) {
                                        Log.debug("Counter greater than 3 teleporting out");
                                        Equipment.Slot.RING.getItem().map(c -> c.click("Grand Exchange"));
                                    }
                                    Log.debug("Ticks: " + tickCounter());
                                    */




                        /*while (!MyRevsClient.myPlayerIsInGE() && !teleblocked) {

                        }*/
                        if (!MyPlayer.isHealthBarVisible()){
                            TeleportManager.teleportOutOfWilderness("PKER DETECTED! Attempting to teleport out!");
                            MyRevsClient.getScript().setState(scripts.rev.State.BANKING);
                        }
                        setHasPkerBeenDetected(true);
                    } else {
                        if (!isAntiPking()) {
                            Log.debug("[DANGER_LISTENER] TELEBLOCKED - Enabling AntiPK");
                            // making it skip the antiPK the thread loop cycle it is set,
                            // to give the main thread a cycle to be able to cancel before we try
                            setAntiPking(true);
                            // we (i lol) returned this.. it should be continue, to just skip the loop, not end the run method
                            continue;
                        }
                    }

                    if (isAntiPking()) {
                        Log.debug("[DANGER_LISTENER] We are anti-pking");
                        antiPk();
                    }
                }

            } else {
                //Log.debug("Out of wilderness. Resetting danger states");
                if (isTeleblocked()) setTeleblocked(false);
                if (hasPkerBeenDetected()) setHasPkerBeenDetected(false);
                if (inDanger()) setInDanger(false);
                if (Mouse.getSpeed() != 200) Mouse.setSpeed(200);
                if (entangleDetecter != null) entangleDetecter = null;
                hasTickCounterStarted = false;
            }
        }
        // if running was set false and thread is ending, run these one last time
        if (isTeleblocked()) setTeleblocked(false);
        if (hasPkerBeenDetected()) setHasPkerBeenDetected(false);
        if (inDanger()) setInDanger(false);
        if (Mouse.getSpeed() != 200) Mouse.setSpeed(200);
        if (entangleDetecter != null) entangleDetecter = null;
        hasTickCounterStarted = false;
    }

    // Could put this in a Utility Class to reuse
    public int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    private boolean isAtRespawn() {
        return MyRevsClient.myPlayerIsDead();
    }

    public static String[] getPvmGear() {
        return PVM_GEAR;
    }

    public static boolean hasTickCounterStarted() {
        return hasTickCounterStarted;
    }

    public static int tickCounter(){
        return tickCounter;
    }


}


/* public static boolean isPkerDetected() {
        var interactables = Query.players().withinCombatLevels(Combat.getWildernessLevel());
        if (interactables.count() != 0) {
            if (Query.players().withinCombatLevels(Combat.getWildernessLevel()).hasSkullIcon().isAny()) {
                Log.info("PKer detected");
                return true;
            }

            if (Query.players().withinCombatLevels(Combat.getWildernessLevel()).isAny()) {
                if (Query.players().isEquipped(22550).isAny() || Query.players().isEquipped(12926).isAny() || Query.players().isEquipped("Viggora's chainmace").isAny() || Query.players().isEquipped("Magic shortbow").isAny() || Query.players().isEquipped("Magic shortbow (i)").isAny()) {
                    Log.info("Pvmer detected");
                    return false;
                } else {
                    Log.info("Possible PKer detected");
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void run() {

        while(running.getAsBoolean()){
            if (RevenantScript.isState(State.WALKING) || RevenantScript.isState(State.KILLING) || RevenantScript.isState(State.LOOTING)) {

                if (PkerDetecter.isPkerDetected()) {
                    quickTele();
                }
            }
            Waiting.wait(50);
        }
    }*/