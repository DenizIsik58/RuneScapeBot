package scripts;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.script.TribotScriptManifest;
import scripts.api.MyDiscordWebhook;
import scripts.api.MyScriptExtension;
import scripts.api.MyScriptVariables;
import scripts.api.utility.MathUtility;
import scripts.api.utility.StringsUtility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@TribotScriptManifest(name = "RevMuler", author = "Deniz", category = "Tools", description = "My muler")


public class MulerScript extends MyScriptExtension {
    private MultiServerSocket muler;
    private String targetSlave = null;
    private boolean hasFinishedCurrentTrade = true;

    private static int totalValue = 0;

    public static AtomicReference<MulerState> state = new AtomicReference<>(MulerState.IDLING);
    private static int namesIndex;
    private static int slavesIndex;
    private List<String> traders = new ArrayList<>();
    private MyDiscordWebhook mulerWebhook;

    public void processTrade(String name) {
        var slaves = MultiServerSocket.getNames();

        for (int i = 0; i < slaves.size(); i++) {
            Log.debug(slaves.get(i));
            Log.debug(name);
             if (StringsUtility.runescapeStringsMatch(slaves.get(i), name)) {
                 if (!traders.contains(name)) {
                     Log.debug(Arrays.toString(MultiServerSocket.getNames().toArray()));
                     Log.debug("Added: " + name + " to the list!");
                     traders.add(name);
                 }
             }
        }


            for (int i = 0; i < slaves.size(); i++) {
                if (hasFinishedCurrentTrade()) {
                    Log.debug("Attempting to add a new target slave for trading");
                    Log.debug("Current target slave: " + getTargetSlave());
                    if (getTargetSlave() == null) {
                        for (int j = 0; j < traders.size(); j++) {
                            Log.debug("Traget is null");
                                Log.debug(traders.get(j));
                                Log.debug(slaves.get(i));
                                if (StringsUtility.runescapeStringsMatch(slaves.get(i), traders.get(j))) {
                                    Log.debug("Found slave target! Trading: " + slaves.get(i));
                                    namesIndex = i;
                                    slavesIndex = j;
                                    setTargetSlave(name);
                                    setHasFinishedCurrentTrade(false);
                                }
                            }
                    }
                }
            }
            Waiting.wait(100);

        setState(MulerState.IDLING);
    }


    @Override
    protected void setupScript(ScriptSetup setup) {


//        we can add other options for script specific settings here.. for now this supports disabling the login handler and/or the break handler and main loop interval
//        like this:
//        setup.setMainLoopInterval(100); (its 50 by default)
        setup.disableWaitForLogin();
//        setup.disableBreakHandler();
        setup.disableLoginHandler();
    }

    @Override
    protected void onStart(String args) {
        mulerWebhook = new MyDiscordWebhook("https://discord.com/api/webhooks/1010438256304861206/ZInuV2pDsUEAELlmxpeVYIIZo6F1MFSmCRacWIoEI16IYQIAKUXikPd40_K_kcNXCiNz");
        Log.debug("Script has been started");
        MessageListening.addTradeRequestListener(this::processTrade);
        Log.debug("Server initiated");
        try {
            muler = new MultiServerSocket();
            new Thread(muler).start();
        } catch (Exception e) {
            Log.debug("Failed creating TCP socket");
        }
        if (!Login.isLoggedIn()) {
            Login.login();
            Waiting.waitNormal(15000, 2000);
        }
    }

    @Override
    protected void onMainLoop() {
        MyScriptVariables.updateStatus(getState().toString());
        switch (getState()) {
            case IDLING:
                Login.logout();
                //scriptSetup.disableWaitForLogin();
                return;
            case MULING:
                //scriptSetup.enableWaitForLogin();
                if (!Login.isLoggedIn()) {
                    Log.debug("Attempting to log in");
                    Login.login();
                }
                handleMuling();
        }
    }

    @Override
    protected void onEnding() {
        Log.debug("Ending script");
        try {
            muler.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static MulerState getState() {
        return state.get();
    }

    public static void setState(MulerState newState) {
        state.set(newState);
    }

    private void handleMuling() {
        if (MultiServerSocket.getNames().size() == 0) {
            setState(MulerState.IDLING);
            return;
        }

        if (getTargetSlave() != null) {
            Log.debug("Target slave: " + getTargetSlave());
            Waiting.waitUntil(10000, () -> Chatbox.acceptTradeRequest(getTargetSlave()));
            Waiting.waitUntil(() -> TradeScreen.OtherPlayer.contains("Coins"));
            var amountOfCoins = TradeScreen.OtherPlayer.getCount("Coins");
            Waiting.waitUntil(() -> {
                TradeScreen.getStage().map(screen -> {
                    if (screen == TradeScreen.Stage.FIRST_WINDOW) {
                        if (TradeScreen.OtherPlayer.hasAccepted()) {
                            TradeScreen.accept();
                            return true;
                        }
                    }
                    return true;
                });
                return false;
            });


            Waiting.waitUntil(25000, () -> {
                TradeScreen.getStage().map(screen -> {
                    if (screen == TradeScreen.Stage.SECOND_WINDOW) {
                        if (TradeScreen.OtherPlayer.hasAccepted()) {
                            TradeScreen.accept();
                            return true;
                        }
                    }
                    return true;
                });
                return false;
            });

            totalValue += amountOfCoins;
            var totalString = MathUtility.getProfitPerHourString(totalValue);
            MyScriptVariables.setProfit(totalString);
            Log.debug("Finished trading: Removing " + MultiServerSocket.getNames().get(namesIndex) + " from the list!");

            MultiServerSocket.getNames().remove(namesIndex);
            traders.remove(slavesIndex);
            setTargetSlave(null);
            Log.debug("Target slave is: " + getTargetSlave());
            setHasFinishedCurrentTrade(true);
            try {
                var screenshot = mulerWebhook.takeScreenShotAndSave("muler");

                mulerWebhook.setUsername("Revenant Muler")
                        .setContent("@everyone **" + MyPlayer.getUsername() + "** has just finished muling - Total gold - **" + Inventory.getCount("Coins") + "**")
                        .addFile(screenshot)
                        .execute();
            }catch (Exception e) {
                Log.error(e);
            }


        }
        Waiting.wait(50);

    }

    private void setTargetSlave(String targetSlave) {
        this.targetSlave = targetSlave;
    }

    public String getTargetSlave() {
        return targetSlave;
    }

    private boolean hasFinishedCurrentTrade() {
        return hasFinishedCurrentTrade;
    }

    public void setHasFinishedCurrentTrade(boolean hasFinishedCurrentTrade) {
        this.hasFinishedCurrentTrade = hasFinishedCurrentTrade;
    }


}
