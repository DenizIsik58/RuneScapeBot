package scripts;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.script.TribotScriptManifest;
import scripts.api.MyDiscordWebhook;
import scripts.api.MyScriptExtension;
import scripts.api.MyScriptVariables;
import scripts.api.utility.MathUtility;
import scripts.api.utility.StringsUtility;

import java.io.IOException;
import java.util.*;
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
    private Set<String> traders = new HashSet<>();
    private MyDiscordWebhook mulerWebhook;

    public void processTrade(String name) {
        var slaves = new ArrayList<>(MultiServerSocket.getNames());
        Log.debug(Arrays.toString(slaves.toArray()));

        if (slaves.size() == 0) {
            setState(MulerState.IDLING);
            return;
        }

        /*for (String slave : slaves) {
            if (StringsUtility.runescapeStringsMatch(slave, name)) {
                if (!traders.contains(name)) {
                    Log.debug(Arrays.toString(MultiServerSocket.getNames().toArray()));
                    Log.debug("Added: " + name + " to the list!");
                    traders.add(name);
                }
            }
        }*/


        for (String slave : slaves) {
            Log.debug("Attempting to add a new target slave for trading");
            Log.debug("Current target slave: " + getTargetSlave());
            Log.debug("Current slave in list: " + slave);

            if (StringsUtility.runescapeStringsMatch(slave, name)) {
                Log.debug("Found slave target! Trading: " + slave);

                Waiting.waitUntil(10000, () -> Chatbox.acceptTradeRequest(name));
                Waiting.waitUntil(() -> TradeScreen.OtherPlayer.contains("Coins"));
                var amountOfCoins = TradeScreen.OtherPlayer.getCount("Coins");
                Waiting.waitUntil(() -> {
                    TradeScreen.getStage().map(screen -> {
                        if (screen == TradeScreen.Stage.FIRST_WINDOW) {
                            if (TradeScreen.OtherPlayer.hasAccepted()) {
                                if (TradeScreen.isDelayed()) {
                                    Waiting.waitUntil(() -> !TradeScreen.isDelayed());
                                }
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
                slaves.forEach(s -> {
                    if (StringsUtility.runescapeStringsMatch(s, name)) {
                        Log.debug("Finished trading: Removing " + s + " from the list!");
                    }
                });

                MultiServerSocket.getNames().remove(slave);

                try {
                    var screenshot = mulerWebhook.takeScreenShotAndSave("muler");

                    mulerWebhook.setUsername("Revenant Muler")
                            .setContent("@everyone **" + MyPlayer.getUsername() + "** has just finished muling - Total gold - **" + Inventory.getCount("Coins") + "**")
                            .addFile(screenshot)
                            .execute();
                } catch (Exception e) {
                    Log.error(e);
                }
            }
                        /*for (int j = 0; j < traders.size(); j++) {
                            Log.debug("Traget is null");

                            }*/


        }

        if (MultiServerSocket.getNames().size() == 0) {
            Log.debug("No slaves left we are idling!");
            setState(MulerState.IDLING);
        }


        Waiting.wait(100);
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
                Waiting.waitNormal(300, 10);
                if (!Login.isLoggedIn()) {
                    Log.debug("Attempting to log in");
                    Login.login();
                }
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
