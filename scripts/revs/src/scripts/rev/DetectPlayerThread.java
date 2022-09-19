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
import org.tribot.script.sdk.walking.LocalWalking;
import org.tribot.script.sdk.walking.WalkState;
import scripts.api.*;
import scripts.api.utility.StringsUtility;

import java.util.List;
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
    private final static String[] PVM_GEAR = new String[]{"Dorgeshuun crossbow","Crystal bow","Abyssal tentacle","Elder maul", "Abyssal whip","Bone crossbow","Red d'hide body", "Black d'hide body", "Rune scimitar", "Toxic blowpipe", "Magic shortbow", "Magic shortbow (i)", "Craw's bow", "Viggora's chainmace"};
    private final static Area FEROX_ENCLAVE = Area.fromRectangle(new WorldTile(3155, 3640, 0), new WorldTile(3120, 3623, 0));
    private final SimpleBooleanProperty running = new SimpleBooleanProperty(false);
    private final AtomicBoolean hasPkerBeenDetected = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private static boolean hasTickCounterStarted = false;
    private static final AtomicBoolean outOfFood = new AtomicBoolean(false);
    private static AtomicBoolean canLogOut = new AtomicBoolean(false);

    private static AtomicBoolean hasHopped = new AtomicBoolean(false);
    @Getter
    @Setter
    private static Projectile lastEntangle = null;

    @Getter
    @Setter
    private static boolean isEntangleTimerStarted = false;
    private static boolean isEntangled = false;
    private static MagicManager entangleDetecter = null;

    private static SuppliesChecker suppliesChecker = null;
    private AtomicBoolean processing = new AtomicBoolean(false);
    private AtomicBoolean inCombatTimer = new AtomicBoolean(false);
    private AtomicBoolean hasStartedEntangleDetecter = new AtomicBoolean(false);
    private AtomicBoolean hasStartedSuppliesChecker = new AtomicBoolean(false);
    public DetectPlayerThread(RevScript revScript) {
        this.script = revScript;
        ScriptListening.addPauseListener(() -> paused.set(true));
        ScriptListening.addResumeListener(() -> paused.set(false));

    }

    private void handleTeleblock() {
        var lastTeleblockNotification = MyScriptVariables.getVariable("lastTeleblockNotification", 0L);

        if (MyRevsClient.myPlayerIsDead() || MyRevsClient.myPlayerIsInGE() || MyRevsClient.myPlayerIsInFerox()) {
            MyScriptVariables.setVariable("lastTeleblockNotification", 0L);
            lastTeleblockNotification = MyScriptVariables.getVariable("lastTeleblockNotification", 0L);

        }
        // 5 mins might be too long and run into going another trip after?
        if (System.currentTimeMillis() - lastTeleblockNotification < (60 * 1000) * 5) {
            setTeleblocked(true);
        } else {
            setTeleblocked(false);
        }

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


    public static boolean detectPkers() {
        Log.debug("Checking for pkers");
        var allPlayers = Query.players()
                .withinCombatLevels(getWildernessLevel())
                .notInArea(FEROX_ENCLAVE)
                .toList();

        if (allPlayers.isEmpty()) {
            Log.debug("Empty pkers");
            return false;
        }



        if (allPlayers.stream().anyMatch(player -> player.isInteractingWithMe())) {
            Log.debug("Interacting with me");
            return true;
        }
        if (allPlayers.stream().allMatch(player -> player.getEquipment().stream().noneMatch(item -> List.of(PVM_GEAR).contains(item.getName())) && (player.getSkullIcon().isEmpty() || player.getSkullIcon().isPresent()))){
            Log.debug("We have a pker");
            return true;
        }

        return false;
    }


    public static boolean canTargetAttackMe(String name) {
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
                if (!Combat.isInWilderness()) {
                    this.cancel();
                }
                Log.debug("Timer is over we are unfrozen!");
                resetFreezeSigns();
            }
        }, 14000);
    }

    public static void handleEatAndPrayer(Player pker) {
        if (isFrozen()) {
                PrayerManager.enablePrayer(Prayer.EAGLE_EYE);
        }else {
                PrayerManager.enablePrayer(Prayer.MYSTIC_MIGHT);
        }


        pker.getEquippedItem(Equipment.Slot.WEAPON).map(Item::getName).ifPresent(playerWeapon -> {
            if (playerWeapon.toLowerCase().contains("staff") || playerWeapon.toLowerCase().contains("wand") || playerWeapon.toLowerCase().contains("trident")) {
                // Magic weapon
                // 1. Set up prayer according to weapon
                PrayerManager.enablePrayer(Prayer.PROTECT_FROM_MAGIC);

            } else if (playerWeapon.toLowerCase().contains("bow") || playerWeapon.toLowerCase().contains("knife") || playerWeapon.toLowerCase().contains("dart") ||(playerWeapon.toLowerCase().contains("ballista"))) {
                // Handle ranging weapon
                // 1. Set up prayer according to weapon
                PrayerManager.enablePrayer(Prayer.PROTECT_FROM_MISSILES);
            } else if (playerWeapon.toLowerCase().contains("bludgeon") || playerWeapon.toLowerCase().contains("sword") || playerWeapon.toLowerCase().contains("dragon") || playerWeapon.toLowerCase().contains("maul") || playerWeapon.toLowerCase().contains("scimitar")) {
                // Handle melee weapon
                // 1. Set up prayer according to weapon
                PrayerManager.enablePrayer(Prayer.PROTECT_FROM_MELEE);
            } else {
                PrayerManager.enablePrayer(Prayer.PROTECT_FROM_MISSILES);
            }
        });

        if (MyAntiBan.shouldEat()) {
            var foodCount = Query.inventory().actionEquals("Eat").count();
            var brewCount = Query.inventory().nameContains("Saradomin brew").count();
            if (foodCount > 0 && brewCount > 0) {
                var comboEat = comboEat(foodCount, true);
                if (comboEat) {
                    MyAntiBan.calculateNextEatPercent();
                }
            } else if (brewCount == 0 && foodCount > 0 || brewCount > 0 && foodCount == 0) {
                if (brewCount == 0) {
                    if (comboEat(foodCount, false)) {
                        MyAntiBan.calculateNextEatPercent();
                    }
                } else {
                    if (eatBrew()) {
                        MyAntiBan.calculateNextEatPercent();
                    }
                }
            } else {
                outOfFood.set(true);
                Log.warn("Out of food under eat percent");
            }
        }
        if (Prayer.getPrayerPoints() < Skill.PRAYER.getActualLevel() - 22) {
            PrayerManager.maintainPrayerPotion();
        }
    }

    private static boolean comboEat(int sharkCount, boolean comboEat) {

        return comboEat ? Query.inventory()
                .actionEquals("Eat")
                .findClosestToMouse()
                .map(c -> c.click("Eat")
                        && Waiting.waitUntil(1000, () -> Query.inventory().actionEquals("Eat").count() < sharkCount))
                .orElse(false) && Query.inventory().nameContains("Saradomin brew").findClosestToMouse().map(brew -> brew.click("Drink")).orElse(false)

                :

                Query.inventory()
                        .nameEquals("Shark")
                        .findClosestToMouse()
                        .map(c -> c.click("Eat")
                                && Waiting.waitUntil(1000, () -> Inventory.getCount("Shark") < sharkCount))
                        .orElse(false);
    }

    private static boolean eatBrew() {
        return Query.inventory().nameContains("Saradomin brew").findClosestToMouse().map(brew -> brew.click("Drink")).orElse(false);
    }

    public void antiPk() {
        var pker = getPker();

        while (Combat.isInWilderness()) {
            MyCamera.init();
            handleTeleblock();
            if (!isTeleblocked()) {
                Log.debug("Teleblock timer is over");
                if (Query.inventory().nameContains("Ring of wealth (").isAny() && !Query.equipment().nameContains("Ring of wealth (").isAny()) {
                    Query.inventory().nameContains("Ring of wealth (").findClosestToMouse().map(c -> c.click("Wear"));
                    Waiting.waitUntil(() -> Query.equipment().nameContains("Ring of wealth (").isAny());
                    Equipment.Slot.RING.getItem().ifPresent(ring -> ring.click("Grand Exchange"));
                }

                if (Query.equipment().nameContains("Ring of wealth (").isAny()) {
                    Equipment.Slot.RING.getItem().ifPresent(ring -> ring.click("Grand Exchange"));
                }
            }

            if (pker != null) {
                Equipment.Slot.RING.getItem().ifPresent(ring -> {
                    if (ring.getId() != 2550) {
                        Query.inventory().nameEquals("Ring of recoil").findClosestToMouse().ifPresent(recoil -> recoil.click("Wear"));
                    }
                });


                if (!isFrozen()) {
                    // Start running
                    Log.debug("I'm not frozen running!");

                    if (MyRevsClient.myPlayerIsInCave()) {
                        WorldTile stairs = new WorldTile(3217, 10058, 0); // Tile to climb up at

                        GlobalWalking.walkTo(stairs, () -> {
                            handleEatAndPrayer(pker);
                            MyOptions.setRunOn();
                            /*if (!canTargetAttackMe(pker.getName())) {

                                // run away if our target is not nearby
                                Log.debug("trying to hop worlds... Target is not in sight");
                                WorldManager.hopToRandomMemberWorldWithRequirements();
                                Waiting.waitUntil(() -> !GameState.isLoading());
                                //TeleportManager.teleportOutOfWilderness("We are trying to teleport out. Target not in sight");
                                //Equipment.Slot.RING.getItem().ifPresent(c -> c.click("Grand Exchange"));
                                Query.inventory().nameContains("Ring of wealth (").findFirst().ifPresent(ring -> ring.click("Wear"));
                                Waiting.waitUntil(() -> Query.equipment().slotEquals(Equipment.Slot.RING).nameContains("Ring of wealth (").isAny());
                                MyExchange.walkToGrandExchange();
                            }*/

                            if (isFrozen()) {
                                Log.debug("I'm frozen returning failure");
                                return WalkState.FAILURE;
                            }
                            Waiting.wait(100);
                            return WalkState.CONTINUE;

                        });
                        handleEatAndPrayer(pker);
                        Query.gameObjects().idEquals(31558).findFirst()
                                .map(c -> c.interact("Climb-up"));
                        Waiting.waitUntil(2000, () -> !MyRevsClient.myPlayerIsInCave());
                    } else {
                        ensureWalkingPermission();
                        //MyExchange.walkToGrandExchange();
                        Log.debug("i'm out of cave running south");

                        MyPlayer.getTile().translate(0, -15).clickOnMinimap();

                        MyOptions.setRunOn();

                        handleEatAndPrayer(pker);
                        Waiting.wait(100);
                    }
                    continue;
                }
            }
            Waiting.wait(100);
        }

    }

    private boolean inCombatTimerHasStarted() {
        return inCombatTimer.get();
    }

    private void setInCombatTimer(boolean timerStarted) {
        inCombatTimer.set(timerStarted);
    }

    public static Optional<Projectile> getProjectile() {
        return Query.projectiles()
                .isTargetingMe()
                .isMoving()
                .graphicIdEquals(178)
                .findFirst();
    }

    public static void proj() {
        getProjectile().ifPresent(prj -> Log.debug("PROJECTILE: " + prj.getGraphicId()));
        getProjectile().ifPresent(p -> Log.debug("START: " + p.getStart()));
        getProjectile().ifPresent(p -> Log.debug("DESTINATION: " + p.getDestination()));
        Log.debug("MY tile: " + MyPlayer.getTile());
    }

    public static void setEntangle() {
        getProjectile().ifPresent(proj -> {
            Log.debug("Setting projectile!");
            lastEntangle = proj;
        });
    }

    public static boolean isFrozen() {
        if (isEntangled) {
            return true;
        }

        if (lastEntangle != null && !isEntangleTimerStarted) {
            var isFrozen = lastEntangle.getDestination().equals(MyPlayer.getTile()) && !MyPlayer.isMoving();
            if (isFrozen) {
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
            Waiting.waitUntil(2000, () -> {
                // this is going to spam xD testing only lol
                Log.trace("Pker thread is waiting for permission to take over walking");
                return !script.isCancellingWalking();
            });
        }
    }

    private void escape(Player pker) {
        setHasPkerBeenDetected(true);
        if (isTeleblocked()) {
            Log.debug("We are teleblocked. Running instead of teleporting");
            return;
        }


        /*if (RevkillerManager.getTarget() != null) {
            if (!RevkillerManager.getTarget().isHealthBarVisible()) {
                Equipment.Slot.RING.getItem().ifPresent(c -> c.click("Grand Exchange"));
                Waiting.waitUntil(MyRevsClient::myPlayerIsInGE);
            }
        }*/


                                        /*if (!MyPlayer.isHealthBarVisible()) {

                                            Equipment.Slot.RING.getItem().ifPresent(c -> c.click("Grand Exchange"));
                                        }*/
        if (!MyRevsClient.myPlayerIsInCave()) {
            Equipment.Slot.RING.getItem().ifPresent(c -> c.click("Grand Exchange"));
            var inGe = Waiting.waitUntil(4000, MyRevsClient::myPlayerIsInGE);
            if (!inGe) {
                Equipment.Slot.RING.getItem().ifPresent(c -> c.click("Grand Exchange"));
            }
            MyRevsClient.getScript().setState(scripts.rev.State.BANKING);

            return;
        }
        double startTime;
        var yCoordDifference = pker.getTile().getY() - MyPlayer.getTile().getY();


            if (pker.getTile().getX() > MyPlayer.getTile().getX() && yCoordDifference >= 5) {
                // Player is north east
                // Run south west
                Log.debug("Player on north east. Running west!");
                //if (!hasTickCounterStarted) {
                WorldTile location = new WorldTile(3202, 10060, 0); // Tile to climb up at
                GlobalWalking.walkTo(location,  () -> {
                    if ((LootingManager.hasPkerBeenDetected() && !Combat.isInWilderness()) || location.isVisible()) {
                        return WalkState.FAILURE;
                    }
                    return WalkState.CONTINUE;
                });
                startTime = GameState.getLoopCycle() / 30D;
                location.clickOnMinimap();

                //Waiting.waitUntil(250, () -> new WorldTile(3205, 10082, 0).clickOnMinimap());
            } else if (pker.getTile().getX() < MyPlayer.getTile().getX() && (yCoordDifference) >= 5) {
                //Player north-west
                // Run east
                Log.debug("Player on north west. Running south east!");
                var location = new WorldTile(3229, 10095, 0);
                GlobalWalking.walkTo(location,  () -> {
                    if ((LootingManager.hasPkerBeenDetected() && !Combat.isInWilderness()) || location.isOnMinimap()) {
                        return WalkState.FAILURE;
                    }
                    return WalkState.CONTINUE;
                });
                startTime = GameState.getLoopCycle() / 30D;
                location.clickOnMinimap();
                //Waiting.waitUntil(250, () -> new WorldTile(3229, 10095, 0).clickOnMinimap());
            } else if ((MyPlayer.getTile().getY() - pker.getTile().getY()) >= 3 && pker.getTile().getX() < MyPlayer.getTile().getX()) {
                // Player south west
                // Run north
                var location = new WorldTile(3226, 10105, 0);
                GlobalWalking.walkTo(location, () -> {
                    if ((LootingManager.hasPkerBeenDetected() && !Combat.isInWilderness()) || location.isOnMinimap()) {
                        return WalkState.FAILURE;
                    }
                    return WalkState.CONTINUE;
                });
                startTime = GameState.getLoopCycle() / 30D;
                location.clickOnMinimap();

            } else {
                var location = new WorldTile(3205, 10082, 0);
                GlobalWalking.walkTo(location,  () -> {
                    if ((LootingManager.hasPkerBeenDetected() && !Combat.isInWilderness()) || location.isOnMinimap()) {
                        return WalkState.FAILURE;
                    }
                    return WalkState.CONTINUE;
                });
                location.clickOnMinimap();
                startTime = GameState.getLoopCycle() / 30D;
                //Waiting.waitUntil(250, () -> new WorldTile(3205, 10082, 0).clickOnMinimap());

        }



        if (!Combat.isInWilderness()) {
            Log.debug("We are not in wilderness");
            return;
        }

        handleEatAndPrayer(pker);

        Equipment.Slot.RING.getItem().ifPresent(ring -> {
            if (ring.isHovering()) {
                Equipment.Slot.RING.getItem().ifPresent(c -> c.hoverMenu("Grand Exchange"));
            }
        });

        if (isTeleblocked()) {
            Log.debug("We are teleblocked. Running instead of teleporting");
            return;
        }

        Log.debug("Timer for teleport has been started");

        var stopTime = startTime + 4D;
        Waiting.waitUntil(() -> GameState.getLoopCycle() / 30D > stopTime);
        //Waiting.wait(2000);
        Log.debug("After waiting: " + GameState.getLoopCycle());
        Log.debug("1,8 seconds gone Teleporting now");
        if (isTeleblocked()) {
            Log.debug("We are teleblocked. Running instead of teleporting");
            return;
        }
        Equipment.Slot.RING.getItem().ifPresent(c -> c.click("Grand Exchange"));
        //MyExchange.walkToGrandExchange();

        var inGE = Waiting.waitUntil(3000, MyRevsClient::myPlayerIsInGE);
        if (!inGE) {
            Equipment.Slot.RING.getItem().ifPresent(c -> c.click("Grand Exchange"));
        }
        if (Equipment.Slot.RING.getItem().isEmpty()){
            Log.debug("Cannot find wealth in equipment. Checking inventory");
            Query.inventory().nameContains("Ring of wealth (").findClosestToMouse().ifPresent(ring -> {
                Log.debug("found it! Wearing it now!");
                ring.click("Wear");
                Waiting.waitUntil(() -> Query.equipment().slotEquals(Equipment.Slot.RING).nameContains("Ring of wealth (").isAny());
            });
        }
        MyRevsClient.getScript().setState(scripts.rev.State.BANKING);

    }

    @Override
    public void run() {
        running.set(true);
        while (running.get()) {
            try {
                Thread.sleep(75);
            } catch (InterruptedException e) {
                Log.debug(e);
                e.printStackTrace();
            }
            if (paused.get()) continue;

            var danger = inDanger();

           //var detectedPkers = detectPkers();

            var detectedPkers = detectPKers();
            var detectedRaggers = detectRaggers();
            var detectedSkull = detectSkull();

            var teleblocked = isTeleblocked();


            handleTeleblock();

            if (Combat.isInWilderness()) {

                if (!hasStartedSuppliesChecker.get()) {
                    if (suppliesChecker == null) {
                        RevkillerManager.resetSuppliesChecks();
                        Log.debug("Supplies checker is null. Starting a new thread");
                        suppliesChecker = new SuppliesChecker();
                        new Thread(suppliesChecker).start();
                        hasStartedSuppliesChecker.set(true);
                    }
                }


                if (!danger) {

                    if (detectedPkers || detectedRaggers || detectedSkull || teleblocked) {
                        if (detectedPkers) Log.warn("[DANGER_LISTENER] DETECTED DANGER - PKER");
                        if (detectedRaggers) Log.warn("[DANGER_LISTENER] DETECTED DANGER - RAGGER");
                        if (teleblocked) Log.warn("[DANGER_LISTENER] DETECTED DANGER - TELEBLOCK");

                        setInDanger(true);
                        setHasPkerBeenDetected(true);
                    }
                    continue;
                }

                danger = inDanger();
                if (danger) {
                    if (!hasStartedEntangleDetecter.get()) {
                        if (entangleDetecter == null) {
                            entangleDetecter = new MagicManager();
                            new Thread(entangleDetecter).start();
                            hasStartedEntangleDetecter.set(true);
                        }
                    }


                    Log.warn("[DANGER_LISTENER] HANDLING DANGER");
                    if (Mouse.getSpeed() == 300) {
                        int dangerMouseSpeed = getRandomNumber(1500, 2000);
                        Mouse.setSpeed(dangerMouseSpeed);
                    }
                    teleblocked = isTeleblocked();
                    Log.debug("Am I teleblocked? " + teleblocked);
                    if (!teleblocked) {
                        Log.trace("[DANGER_LISTENER] NOT TELEBLOCKED - Teleporting");

                        if (isAntiPking()) {
                            setAntiPking(false);
                        }


                            Log.debug("Proccessing: " + processing.get());

                        if (!processing.get()) {

                            processing.set(true);


                        if (detectedSkull) {
                            Log.debug("Skull detected");
                            var skulled = Query.players()
                                    .withinCombatLevels(getWildernessLevel())
                                    .notInArea(FEROX_ENCLAVE)
                                    .isNotEquipped(PVM_GEAR)
                                    .hasSkullIcon().findFirst().orElse(null);

                            if (skulled != null) {
                                escape(skulled);
                                processing.set(false);
                                continue;
                            }

                            TeleportManager.teleportOut();
                            processing.set(false);
                            continue;
                        }

                            if (detectedPkers) {
                                Log.debug("Pkers detected");
                                var possiblePker = Query.players()
                                        .withinCombatLevels(getWildernessLevel())
                                        .notInArea(FEROX_ENCLAVE)
                                        .isNotEquipped(PVM_GEAR)
                                        .findFirst().orElse(null);

                                Log.debug("ESCAPING PROCESS HAS BEEN STARTED");

                                if (possiblePker != null) {
                                    escape(possiblePker);
                                    processing.set(false);
                                    continue;
                                }

                                TeleportManager.teleportOut();


                                MyRevsClient.getScript().setState(scripts.rev.State.BANKING);
                            }

                            if (isTeleblocked()) {
                                processing.set(false);
                                continue;
                            }

                            if (detectedRaggers){
                                Log.debug("Raggers detected");
                                var ragger = Query.players()
                                        .withinCombatLevels(getWildernessLevel())
                                        .hasSkullIcon()
                                        .notInArea(FEROX_ENCLAVE)
                                        .isInteractingWithMe()
                                        .findFirst().orElse(null);

                                if (ragger != null) {
                                    handleEatAndPrayer(ragger);
                                    escape(ragger);
                                    processing.set(false);
                                    continue;
                                }

                                TeleportManager.teleportOut();
                                MyRevsClient.getScript().setState(scripts.rev.State.BANKING);

                            }

                            TeleportManager.teleportOut();


                            processing.set(false);

                        }

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
                resetDangerSigns();
                MyRevsClient.getScript().killPkThread();
            }
        }
        // if running waFs set false and thread is ending, run these one last time
        resetDangerSigns();
    }

    public void resetDangerSigns(){
        if (isTeleblocked()) setTeleblocked(false);
        if (hasPkerBeenDetected()) setHasPkerBeenDetected(false);
        if (inDanger()) setInDanger(false);
        if (Mouse.getSpeed() != 300) Mouse.setSpeed(300);
        if (entangleDetecter != null) entangleDetecter = null;
        hasTickCounterStarted = false;
        setHasHopped(false);
        Log.debug("Setting supplies checker to null");
        if (suppliesChecker != null) suppliesChecker = null;
        if (hasStartedEntangleDetecter.get()) hasStartedEntangleDetecter.set(false);
        if (hasStartedSuppliesChecker.get()) hasStartedSuppliesChecker.set(false);
    }

    private static void resetFreezeSigns(){
        lastEntangle = null;
        isEntangleTimerStarted = false;
        isEntangled = false;
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

    public static boolean getHasHopped() {
        return hasHopped.get();
    }

    private void setHasHopped(boolean hopped) {
        hasHopped.set(hopped);
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