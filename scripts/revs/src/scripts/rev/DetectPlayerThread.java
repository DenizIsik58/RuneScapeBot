package scripts.rev;


import org.tribot.script.sdk.*;
import org.tribot.script.sdk.input.Mouse;
import org.tribot.script.sdk.interfaces.ServerMessageListener;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Area;
import org.tribot.script.sdk.types.Player;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import org.tribot.script.sdk.walking.WalkState;
import scripts.api.MyExchange;
import scripts.api.MyScriptVariables;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.tribot.script.sdk.Combat.getWildernessLevel;


public class DetectPlayerThread extends Thread {

    private final AtomicBoolean teleblocked = new AtomicBoolean(false);
    private final AtomicBoolean danger = new AtomicBoolean(false);
    private final AtomicBoolean dangerFlag = new AtomicBoolean(false);
    private final AtomicBoolean waitingForDeath = new AtomicBoolean(false);
    private final static String[] PVM_GEAR = new String[]{"Toxic blowpipe","Magic shortbow","Magic shortbow (i)","Craw's bow", "Viggora's chainmace" };
    private final static Area FEROX_ENCLAVE = Area.fromRectangle(new WorldTile(3155, 3640, 0), new WorldTile(3116, 3623, 0));
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean hasPkerBeenDetected = new AtomicBoolean(false);
    private boolean isAntiPkEnabled = false;
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private final ServerMessageListener serverMessageListener = message -> {
        if (!Combat.isInWilderness()) {
            if (isTeleblocked()) setTeleblocked(false);
            if (hasPkerBeenDetected()) setHasPkerBeenDetected(false);
            if (inDangerFlag()) setDangerFlag(false);
            if (inDanger()) setInDanger(false);
            if (Mouse.getSpeed() != 200) Mouse.setSpeed(200);
        } else {
            if (message.contains("A Tele Block spell has been cast on you")) {
                setTeleblocked(true);
                Log.warn("[DANGER_LISTENER] WE ARE TBED");
            }
        }
    };

    public DetectPlayerThread() {
        ScriptListening.addPauseListener(() -> paused.set(true));
        ScriptListening.addResumeListener(() -> paused.set(false));
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

    public boolean isAntiPking() {
        return waitingForDeath.get();
    }

    public void antiPking(boolean value) {
        waitingForDeath.set(value);
    }

    public boolean inDangerFlag() {
        return dangerFlag.get();
    }

    public void setDangerFlag(boolean value) {
        dangerFlag.set(value);
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

    private void hookupLisener() {
        MessageListening.addServerMessageListener(serverMessageListener);
    }

    private void disconnectListener() {
        MessageListening.removeServerMessageListener(serverMessageListener);
    }

    public void setHasPkerBeenDetected(boolean hasPkerBeenDetected) {
        this.hasPkerBeenDetected.set( hasPkerBeenDetected);
    }

    public boolean hasPkerBeenDetected() {
        return hasPkerBeenDetected.get();
    }


    public static Player getPker() {
        var name = getPkerName();
        if (name.isEmpty()) return null;
        return Query.players().nameEquals(name).findFirst().orElse(null);
    }

    public static String getPkerName() {
        return MyScriptVariables.getVariable("pkerName", "");
    }

    public static void setTargetName(String targetName) {
        //DENIZ: we dont need this, but future reference, since the argument name is the same as the variable name
        // you have to use this.targetName = targetName;
        targetName = targetName;

    }

    public static void handleEatAndPrayer(){
        if (MyPlayer.getCurrentHealthPercent() < 60 && Inventory.contains("Shark")){
            Query.inventory().nameEquals("Shark").findClosestToMouse().map(c -> c.click("Eat"));
        }
        if (Prayer.getPrayerPoints() < 15) {
            PrayerManager.sipPrayer();
        }
    }

    public void antiPk(){
        var pker = getPker();
        if (pker == null || !isTeleblocked()) {
            Log.debug("trying to hop worlds... Target is not in sight");
            WorldManager.hopToRandomMemberWorldWithRequirements();
            TeleportManager.teleportOutOfWilderness("We are trying to teleport out. Target not in sight");

            // TODO: Try to run away? Once it is activated. We know a pker has been on us.
            return;
        }
        // pker will not be null from here on  just use pker now instead of getPker
        handleEatAndPrayer();
        var playerWeapon = pker.getEquippedItem(Equipment.Slot.WEAPON).get().getName();
        // We are tbed or our target is still here. Fight him
        if (playerWeapon.toLowerCase().contains("staff")){
            // Magic weapon
            // 1. Set up prayer according to weapon
            PrayerManager.setPrayer(Prayer.PROTECT_FROM_MAGIC);

        }else if (playerWeapon.toLowerCase().contains("dragon") || playerWeapon.toLowerCase().contains("maul") || playerWeapon.toLowerCase().contains("scimitar")){
            // Handle melee weapon
            // 1. Set up prayer according to weapon
            PrayerManager.setPrayer(Prayer.PROTECT_FROM_MELEE);
        }else if (playerWeapon.toLowerCase().contains("crossbow")){
            // Handle ranging weapon
            // 1. Set up prayer according to weapon
            PrayerManager.setPrayer(Prayer.PROTECT_FROM_MISSILES);
        }
        // 2. Fight back pker if not
        PrayerManager.setPrayer(Prayer.PROTECT_ITEMS);
        if (Query.players().nameEquals(pker.getName()).isMyPlayerNotInteractingWith().isAny()){
            // Our player is not attacking him.
            pker.click("Attack");
        }
        WorldTile stairs = new WorldTile(3217, 10058, 0); // Tile to climb up at
        if (MyRevsClient.myPlayerIsInCave()){
            GlobalWalking.walkTo(stairs, () -> {
                Query.gameObjects().idEquals(31558).findBestInteractable().map(c -> c.interact("Climb-up"));
                return WalkState.CONTINUE;
            });
        }

        if (!MyRevsClient.myPlayerIsInCave()){
            MyExchange.walkToGrandExchange();
        }

        // 3. try to run away if we are not frozen


    }

    @Override
    public void run() {
        running.set(true);
        hookupLisener();
        while (running.get()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Log.debug(e);
                e.printStackTrace();
            }
            Log.debug(paused.get());
            if (paused.get()) continue;


            if (Combat.isInWilderness()) {
                Log.debug("I'm detecting...");
                if (!inDanger() && !inDangerFlag()) {
                    if (detectPKers() || detectRaggers() || detectSkull()) {
                        // We are in danger here.
                        Log.warn("[DANGER_LISTENER] We are in danger!! A potential pker, ragger, or already skulled player has been spotted!");
                        setInDanger(true);
                        setDangerFlag(true);
                        setHasPkerBeenDetected(true);
                    }
                }
                if (inDanger() || inDangerFlag() || isTeleblocked()) {
                    Log.trace("PKER");
                    if (Mouse.getSpeed() == 200) {
                        int dangerMouseSpeed = getRandomNumber(1500, 2000);
                        Mouse.setSpeed(dangerMouseSpeed);
                        Log.warn("[DANGER_LISTENER] DANGER MOUSE SPEED SET AT: " + dangerMouseSpeed);
                    }


                    if (!isTeleblocked()) {
                        Log.trace("Not Teleblocked");
                        if (isAntiPking()) {
                            antiPking(false);
                        }
                        //TeleportManager.teleportOutOfWilderness("PKER DETECTED! Attempting to teleport out!");

                    } else {
                        Log.trace("Teleblocked");
                        antiPking(true);
                    }

                    if (isAntiPking()) {
                       antiPk();
                    }
                }

            }
        }

        disconnectListener();
    }

    // Could put this in a Utility Class to reuse
    public int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    private boolean isAtRespawn() {
        return MyRevsClient.myPlayerIsDead();
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