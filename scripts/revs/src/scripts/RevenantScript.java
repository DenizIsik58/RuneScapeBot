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

@TribotScriptManifest(name = "revs", author = "Deniz", category = "Moneymaking", description = "revs")

public class RevenantScript implements TribotScript {
    public static State state;
    public static WorldTile selectedMonsterTile = new WorldTile(3160, 10115,0 );
    private static MulingClient socketClient;
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Override
    public void configure(@NotNull ScriptConfig config) {
        config.setBreakHandlerEnabled(true);
        config.setRandomsAndLoginHandlerEnabled(true);
    }


    private void processMessage(String message) {
        if (message.equals("<col=ef1020>Your weapon has run out of revenant ether.</col>")){
            RevenantScript.state = State.BANKING;
            return;
        }
        if (message.equals("<col=ef1020>The effects of the divine potion have worn off.")){
            BoostingManager.resetBoost();
            return;
        }
        if (message.equals("You don't have enough inventory space.")){
            Bank.depositInventory();
            BankManagerRevenant.withdrawGear();
            return;
        }

        try {
            var content = message.split(" ");
            var type = content[1];
            if (type.equals("bracelet")) {
                if (message.contains("it will not absorb")){
                    EquipmentManager.toggleBraceletAbsorbOn();
                }
                // Update bracelet charges
                EquipmentManager.setBraceCharges(Integer.parseInt(content[3]));
            } else if (type.equals("bow")) {
                // update bow charges
                EquipmentManager.setBowCharges(Integer.parseInt(content[3].replace(",", "")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @SneakyThrows
    @Override
    public void execute(@NotNull String args) {
        ScriptListening.addEndingListener(() -> running.set(false));
        MessageListening.addServerMessageListener(this::processMessage);
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
        if (Options.isAllSettingsOpen()){
            Options.closeAllSettings();
        }
        GameTab.setSwitchPreference(GameTab.SwitchPreference.KEYS);
        state = State.STARTING;
        Mouse.setSpeed(200);
        socketClient = new MulingClient();

        try{
            MulingClient.startConnection("127.0.0.1", 6668);
        }catch (Exception e){

        }


        if (!Login.isLoggedIn()){
            Login.login();
            Waiting.waitUntil(Login::isLoggedIn);
        }

        //var antiPkThread = new Thread(new PkerDetecter(running::get));
        new Thread(new DetectPlayerThread(running::get)).start();
        //antiPkThread.start();

        while (true) {

            if (MyRevsClient.myPlayerIsInGE() && DetectPlayerThread.isHasPkerBeenDetected()){
                RevenantScript.state = State.BANKING;
                Log.debug("Banking after pk detection");
                DetectPlayerThread.setHasPkerBeenDetected(false);
            }

            if (DetectPlayerThread.isHasPkerBeenDetected()){
                Log.info("Pker detected.");
                Waiting.wait(50);
                continue;
            }


            OptionsManager.setRunOn();


            if (!Login.isLoggedIn()){
                Login.login();
            }

            if (state == State.STARTING) {
                if (!MyRevsClient.myPlayerIsInGE()) {
                    if (!Bank.isNearby()) {
                        Query.equipment().nameContains("Ring of wealth (").findFirst().map(ring -> ring.click("Grand exchange"));
                    }
                }
                BankManagerRevenant.init();
                state = State.WALKING;
                Log.debug(state);
            }

            if (state == State.WALKING) {
                var tile = TeleportManager.refill();

                selectedMonsterTile = tile;
                if (tile.isRendered() || tile.isVisible() || tile.isInLineOfSight()) {
                    OptionsManager.init();
                    CameraManager.init();
                    PrayerManager.init();
                    LootingManager.resetTripValue();
                    RevkillerManager.setiWasFirst(false);
                    BoostingManager.resetBoost();
                    state = State.KILLING;
                    Log.debug(state);
                }
            }

            if (state == State.KILLING) {
                RevkillerManager.killMonster();
            }

            if (state == State.BANKING) {
                Log.debug(state);
                Log.info("Total amount made this trip: " + LootingManager.getTripValue());
                Log.info("Total amount made since script start: " + LootingManager.getTotalValue());
                Log.info("Total times died so far: " + DeathManger.totalDeaths());
                PrayerManager.disableQuickPrayer();
                DetectPlayerThread.setHasPkerBeenDetected(false);
                TeleportManager.setHasVisitedBeforeTrip(false);
                BankManagerRevenant.bankLoot();
            }

            if (state == State.LOOTING) {
                LootingManager.loot();
            }

            if (state == State.DEATH) {
                Log.debug(state);
                TeleportManager.setHasVisitedBeforeTrip(false);
                DeathManger.incrementTotalDeaths();
                Log.info("Oh dear! You have just died with: " + LootingManager.getTripValue() + " Gold!! BASTARD");
                Log.info("Total times died so far: " + DeathManger.totalDeaths());
                LootingManager.setTotalValue(LootingManager.getTotalValue() - LootingManager.getTripValue());
                LootingManager.resetTripValue();
                DeathManger.reGearFromDeath();
            }


            // RUN BOT
            Waiting.wait(50);
        }
    }

    public static MulingClient getSocketClient() {
        return socketClient;
    }
}
