package scripts.rev;


import org.tribot.script.sdk.*;
import org.tribot.script.sdk.input.Mouse;
import org.tribot.script.sdk.interfaces.ServerMessageListener;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Area;
import org.tribot.script.sdk.types.WorldTile;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.tribot.script.sdk.Combat.getWildernessLevel;


public class DetectPlayerThread extends Thread {

    private final AtomicBoolean teleblocked = new AtomicBoolean(false);
    private final AtomicBoolean danger = new AtomicBoolean(false);
    private final AtomicBoolean dangerFlag = new AtomicBoolean(false);
    private final AtomicBoolean waitingForDeath = new AtomicBoolean(false);
    private final static String[] PVM_GEAR = new String[]{"Toxic blowpipe","Magic shortbow","Magic shortbow (i)","Craw's bow", "Viggora's chainmace" };
    private final static Area FEROX_ENCLAVE = Area.fromRectangle(new WorldTile(3155, 3640, 0), new WorldTile(3116, 3623, 0));
    private static BooleanSupplier running;
    private static boolean hasPkerBeenDetected;

    private final AtomicBoolean paused = new AtomicBoolean(false);

    private final ServerMessageListener serverMessageListener = message -> {
        if (!Combat.isInWilderness()) {
            if (isTeleblocked()) setTeleblocked(false);
            if (hasPkerBeenDetected()) setHasPkerBeenDetected(false);
            if (inDangerFlag()) setDangerFlag(false);
            if (inDanger()) setInDanger(false);
            if (Mouse.getSpeed() != 300) Mouse.setSpeed(300);
        } else {
            if (message.contains("A Tele Block spell has been cast on you")) {
                setTeleblocked(true);
                Log.warn("[DANGER_LISTENER] WE ARE TBED");
            }
        }
    };

    public DetectPlayerThread(BooleanSupplier running) {
                    DetectPlayerThread.running = running;
                    ScriptListening.addPauseListener(() -> paused.set(true));
                    ScriptListening.addResumeListener(() -> paused.set(false));
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

    public boolean isWaitingForDeath() {
        return waitingForDeath.get();
    }

    public void setWaitingForDeath(boolean value) {
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
        DetectPlayerThread.hasPkerBeenDetected = hasPkerBeenDetected;
    }

    public boolean hasPkerBeenDetected() {
        return hasPkerBeenDetected;
    }

    @Override
    public void run() {
        hookupLisener();
        while (running.getAsBoolean()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (paused.get()) continue;


            if (Combat.isInWilderness()) {
                if (!inDanger() && !inDangerFlag()) {
                    if (detectPKers() || detectRaggers() || detectSkull()) {
                        // We are in danger here.
                        Log.warn("[DANGER_LISTENER] We are in danger!! A pker, ragger or skulled person has been spotted!");
                        setInDanger(true);
                        setDangerFlag(true);
                        setHasPkerBeenDetected(true);
                    }
                }
                if (inDanger() || inDangerFlag() || isTeleblocked()) {
                    Log.trace("PKER");
                    if (Mouse.getSpeed() == 300) {
                        int dangerMouseSpeed = getRandomNumber(800, 2000);
                        Mouse.setSpeed(dangerMouseSpeed);
                        Log.warn("[DANGER_LISTENER] DANGER MOUSE SPEED SET AT: " + dangerMouseSpeed);
                    }

                    if (!isTeleblocked()) {
                        Log.trace("Not Teleblocked");
                        if (isWaitingForDeath()) {
                            setWaitingForDeath(false);
                        }
                        TeleportManager.teleportOutOfWilderness("PKER DETECTED! Attempting to teleport out!");
                        return;
                    } else {
                        Log.trace("Teleblocked");
                        setWaitingForDeath(true);
                    }

                    if (isWaitingForDeath()) {
                        Log.trace("Waiting for death");
                        if (!Prayer.PROTECT_ITEMS.isEnabled()) {
                            Prayer.PROTECT_ITEMS.enable();
                        }
                    }
                }

            }else {
                setDangerFlag(false);
                setHasPkerBeenDetected(false);
                setInDanger(false);
                setTeleblocked(false);
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