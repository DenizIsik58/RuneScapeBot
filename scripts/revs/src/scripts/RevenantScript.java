package scripts;


import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.antiban.Antiban;
import org.tribot.script.sdk.antiban.AntibanProperties;
import org.tribot.script.sdk.input.Mouse;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.script.ScriptConfig;
import org.tribot.script.sdk.script.TribotScript;
import org.tribot.script.sdk.script.TribotScriptManifest;
import org.tribot.script.sdk.types.WorldTile;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@TribotScriptManifest(name = "revs", author = "Deniz", category = "Moneymaking", description = "revs")

public class RevenantScript implements TribotScript {
    public static AtomicReference<State> state = new AtomicReference<>(State.STARTING);
    public static WorldTile selectedMonsterTile = new WorldTile(3160, 10115,0 );
    private static MulingClient socketClient;
    private final AtomicBoolean running = new AtomicBoolean(true);



    @Override
    public void configure(@NotNull ScriptConfig config) {
        config.setBreakHandlerEnabled(true);
        config.setRandomsAndLoginHandlerEnabled(true);
    }

    @SneakyThrows
    @Override
    public void execute(@NotNull String args) {
        ScriptListening.addEndingListener(() -> running.set(false));
        MessageListening.addServerMessageListener(MyRevsClient::processMessage);
        ScriptListening.addPreEndingListener(() -> {
            try {
                if (Combat.isInWilderness()){
                    PkerDetecter.quickTele();
                }
                socketClient.stopConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


        OptionsManager.init();
        CameraManager.init();
        PrayerManager.init();
        AntiBanManager.init();
        /*if (Options.isAllSettingsOpen()){
            Options.closeAllSettings();
        }*/
        GameTab.setSwitchPreference(GameTab.SwitchPreference.KEYS);
        setState(State.STARTING);
        Mouse.setSpeed(200);
        socketClient = new MulingClient();

        try{
            MulingClient.startConnection("127.0.0.1", 6668);
        }catch (Exception e){

        }

        MyRevsClient.waitUntilLoggedIn();

        //var antiPkThread = new Thread(new PkerDetecter(running::get));
        new Thread(new DetectPlayerThread(running::get)).start();
        //antiPkThread.start();

        while (running.get()) {
            Waiting.wait(50);

            updateState();

            if (DetectPlayerThread.isHasPkerBeenDetected()){
                Log.info("Pker detected.");
                continue;
            }



            OptionsManager.setRunOn();

            switch(getState()) {
                case STARTING:
                    handleStarting();
                    break;
                case BANKING:
                    handleBanking();
                    break;
                case WALKING:
                    handleWalking();
                    break;
                case SELLLOOT:
                    handleSellLoot();
                    break;
                case KILLING:
                    handleKilling();
                    break;
                case DEATH:
                    handleDeath();
                    break;
                case LOOTING:
                    handleLooting();
                    break;
            }
        }
    }

    private static void updateState() {
        if (isState(State.STARTING)) return;
        if (MyRevsClient.myPlayerIsInGE() && !isState(State.BANKING))  setState(State.BANKING);
        if (MyRevsClient.myPlayerIsInGE()) setState(State.BANKING); // Do we need this?
        if (MyRevsClient.myPlayerIsDead()){
            DetectPlayerThread.setHasPkerBeenDetected(false);
            setState(State.DEATH);
        }
        if (MyRevsClient.myPlayerIsInFerox()) {
            // if not bank task is satisfied
                // teleport to ge
            // else
            if (!BankManagerRevenant.isEquipmentBankTaskSatisfied()){
                BankManagerRevenant.goToGeIfNotThere();
            }else {
                setState(State.WALKING);
            }
        }
    }

    private static void handleSellLoot() {
        Log.warn("NOT YET IMPLEMENTED");
    }

    private static void handleStarting() {
        BankManagerRevenant.goToGeIfNotThere();
        BankManagerRevenant.init();
        setState(State.WALKING);
        Log.debug(state);
    }

    private static void handleWalking() {
        var tile = TeleportManager.refill();
        selectedMonsterTile = tile;
        if (TeleportManager.monsterTileIsDetected(tile)) {
            OptionsManager.init();
            CameraManager.init();
            PrayerManager.init();
            LootingManager.resetTripValue();
            RevkillerManager.setiWasFirst(false);
            BoostingManager.resetBoost();
            setState(State.KILLING);
            Log.debug(state);
        }
    }

    private static void handleKilling() {
        RevkillerManager.killMonster();
    }

    private static void handleBanking() {
        Log.debug(state);
        Log.info("Total amount made this trip: " + LootingManager.getTripValue());
        Log.info("Total amount made since script start: " + LootingManager.getTotalValue());
        Log.info("Total times died so far: " + DeathManger.totalDeaths());
        PrayerManager.disableQuickPrayer();
        DetectPlayerThread.setHasPkerBeenDetected(false);
        TeleportManager.setHasVisitedBeforeTrip(false);
        BankManagerRevenant.bankLoot();
    }

    private static void handleLooting() {
        LootingManager.loot();

    }

    private static void handleDeath() {
        Log.debug(state);
        TeleportManager.setHasVisitedBeforeTrip(false);
        DeathManger.incrementTotalDeaths();
        Log.info("Oh dear! You have just died with: " + LootingManager.getTripValue() + " Gold!! BASTARD");
        Log.info("Total times died so far: " + DeathManger.totalDeaths());
        LootingManager.setTotalValue(LootingManager.getTotalValue() - LootingManager.getTripValue());
        LootingManager.resetTripValue();
        DeathManger.reGearFromDeath();
    }

    public static State getState() {
        return state.get();
    }

    public static void setState(State state) {
        RevenantScript.state.set(state);
    }

    public static boolean isState(State state) {
        return getState().equals(state);
    }

    public static MulingClient getSocketClient() {
        return socketClient;
    }
}
