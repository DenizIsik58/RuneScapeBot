package scripts;


import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.input.Mouse;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.script.ScriptConfig;
import org.tribot.script.sdk.script.TribotScript;
import org.tribot.script.sdk.script.TribotScriptManifest;
import org.tribot.script.sdk.types.WorldTile;

import java.io.IOException;

@TribotScriptManifest(name = "revs", author = "Deniz", category = "Moneymaking", description = "revs")

public class RevenantScript implements TribotScript {
    public static State state;
    public static WorldTile selectedMonsterTile = new WorldTile(3160, 10115,0 );
    private static MulingClient socketClient;

    @Override
    public void configure(@NotNull ScriptConfig config) {
        config.setBreakHandlerEnabled(true);
        config.setRandomsAndLoginHandlerEnabled(true);
    }


    private void processMessage(String message) {
        if (message.equals("The effects of the divine potion have worn off.")){
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
        MessageListening.addServerMessageListener(this::processMessage);
        ScriptListening.addPreEndingListener(() -> {
            try {
                socketClient.stopConnection();
                Log.info("Connection to mule has been closed!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        OptionsManager.init();
        CameraManager.init();
        PrayerManager.init();
        state = State.STARTING;
        Mouse.setSpeed(200);
        socketClient = new MulingClient();

        try{
            socketClient.startConnection("127.0.0.1", 6668);
        }catch (Exception e){
            Log.info("The muling server is offline!");
        }
        Log.info(Login.isLoggedIn());
        if (!Login.isLoggedIn()){
            Log.info("LOGGING IND");
            Login.login();
            Waiting.waitUntil(Login::isLoggedIn);
        }
        new Thread(new PkerDetecter()).start();
        while (true) {
            if (!Login.isLoggedIn()){
                Login.login();
            }
           //Log.info(state.name());
            if (state == State.STARTING) {
                if (!MyRevsClient.myPlayerIsInGE()) {
                    if (!Bank.isNearby()) {
                        Query.equipment().nameContains("Ring of wealth (").findFirst().map(ring -> ring.click("Grand exchange"));
                    }
                }
                BankManagerRevenant.init();
                state = State.WALKING;
            }

            if (state == State.WALKING) {
                Log.info(state);
                var tile = TeleportManager.refillAndWalkToCave();
                selectedMonsterTile = tile;
                if (tile.isRendered() || tile.isVisible()) {
                    OptionsManager.init();
                    CameraManager.init();
                    PrayerManager.init();
                    state = State.KILLING;
                    LootingManager.resetTripValue();
                    RevkillerManager.setiWasFirst(false);
                }
            }
            if (state == State.KILLING) {
                Log.info(state);
                RevkillerManager.killMonster();
                if (MyRevsClient.myPlayerIsDead()) {
                    state = State.DEATH;
                }
            }

            if (state == State.BANKING) {
                Log.info(state);
                PrayerManager.disableQuickPrayer();
                BankManagerRevenant.bankLoot();
            }

            if (state == State.LOOTING) {
                Log.info(state);
                LootingManager.loot();
            }

            if (state == State.DEATH) {
                DeathManger.reGearFromDeath();
                state = State.BANKING;
            }

            if (state == State.SELLLOOT){
                GrandExchangeRevManager.sellLoot();
            }
            // RUN BOT
            Waiting.wait(50);
        }
    }

    public static MulingClient getSocketClient() {
        return socketClient;
    }
}
