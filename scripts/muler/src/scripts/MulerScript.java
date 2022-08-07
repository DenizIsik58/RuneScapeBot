package scripts;

import org.jetbrains.annotations.NotNull;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.painting.Painting;
import org.tribot.script.sdk.painting.template.basic.BasicPaintTemplate;
import org.tribot.script.sdk.painting.template.basic.PaintLocation;
import org.tribot.script.sdk.painting.template.basic.PaintRows;
import org.tribot.script.sdk.script.ScriptConfig;
import org.tribot.script.sdk.script.TribotScriptManifest;
import scripts.api.MyClient;
import scripts.api.MyScriptExtension;
import scripts.api.MyScriptVariables;
import scripts.api.utility.StringsUtility;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@TribotScriptManifest(name = "RevMuler", author = "Deniz", category = "Tools", description = "My muler")


public class MulerScript extends MyScriptExtension {
    private MultiServerSocket muler;
    private String targetSlave = null;
    private boolean hasFinishedCurrentTrade = true;
    private ScriptSetup scriptSetup;

    public static AtomicReference<MulerState> state = new AtomicReference<>(MulerState.IDLING);
    private static int index;

    public void processTrade(String name) {

        var slaves = MultiServerSocket.getNames();
        slaves.forEach(Log::debug);
        Log.debug(name);
        Log.debug(getTargetSlave());

        while (slaves.size() != 0) {
            for (int i = 0; i <= slaves.size() - 1; i++) {
                if (hasFinishedCurrentTrade()) {
                    if (getTargetSlave() == null) {
                        if (StringsUtility.runescapeStringsMatch(slaves.get(i), name)) {
                            Log.debug("Found slave target! Trading: " + slaves.get(i));
                            index = i;
                            setTargetSlave(slaves.get(i));
                            setHasFinishedCurrentTrade(false);
                        }
                    }
                }
            }
            Waiting.wait(100);
        }
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
        Log.debug("Script has been started");
        MessageListening.addTradeRequestListener(this::processTrade);
        Log.debug("Server initiated");
        try {
            muler = new MultiServerSocket();
            new Thread(muler).start();
        } catch (Exception e) {
            Log.debug("Failed creating TCP socket");
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

        while (MultiServerSocket.getNames().size() != 0) {
            Log.debug(getTargetSlave());
            if (getTargetSlave() != null) {
                Log.debug("Target slave: " + getTargetSlave());
                Waiting.waitUntil(100000, () -> Chatbox.acceptTradeRequest(getTargetSlave()));
                Waiting.waitUntil(() -> TradeScreen.OtherPlayer.contains("Coins"));
                Waiting.waitUntil(() -> {
                    TradeScreen.getStage().map(screen -> {
                        if (screen == TradeScreen.Stage.FIRST_WINDOW) {
                            if (TradeScreen.OtherPlayer.hasAccepted()) {
                                TradeScreen.accept();
                                return true;
                            }
                        }
                        return false;
                    });
                    return false;
                });


                Waiting.waitUntil(() -> {
                    TradeScreen.getStage().map(screen -> {
                        if (screen == TradeScreen.Stage.SECOND_WINDOW) {
                            if (TradeScreen.OtherPlayer.hasAccepted()) {
                                TradeScreen.accept();
                                return true;
                            }
                        }
                        return false;
                    });
                    return false;
                });
                MultiServerSocket.getNames().remove(index);
                setTargetSlave(null);
                setHasFinishedCurrentTrade(true);
            }
            Waiting.wait(50);
        }

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
