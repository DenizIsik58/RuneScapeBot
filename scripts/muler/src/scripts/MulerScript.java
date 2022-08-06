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

import java.io.IOException;

@TribotScriptManifest(name = "RevMuler", author = "Deniz", category = "Tools", description = "My muler")


public class MulerScript extends MyScriptExtension {
    private static MulerState state;
    private MultiServerSocket muler;
    private String targetSlave = null;
    private boolean hasFinishedCurrentTrade = true;
    private ScriptSetup scriptSetup;

    public void processTrade(String name) {

        var slaves = MultiServerSocket.getNames();

        while (slaves.size() != 0) {

            if (hasFinishedCurrentTrade()) {
                if (getTargetSlave() == null) {
                    setTargetSlave(slaves.get(0));
                    setHasFinishedCurrentTrade(false);
                }
            }

            Waiting.wait(100);
        }
        state = MulerState.IDLING;
    }


    @Override
    protected void setupScript(ScriptSetup setup) {
        //setup.disableWaitForLogin();
        //setup.disableBreakHandler();
        scriptSetup = setup;
    }

    @Override
    protected void onStart(String args) {
        Log.info("Script has been started");
        MessageListening.addTradeRequestListener(this::processTrade);
        Log.info("Server initiated");
        state = MulerState.IDLING;
        muler = new MultiServerSocket();
        new Thread(muler).start();

    }

    @Override
    protected void onMainLoop() {

        switch (getState()) {
            case IDLING:
                scriptSetup.disableWaitForLogin();
                return;
            case MULING:
                scriptSetup.enableWaitForLogin();
                MyClient.waitUntilLoggedIn();
                handleMuling();
        }
    }

    @Override
    protected void onEnding() {
        ScriptListening.addPreEndingListener(() -> {
            try {
                muler.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    public static MulerState getState() {
        return state;
    }

    public static void setState(MulerState state) {
        MulerScript.state = state;
    }

    private void handleMuling() {
        if (getTargetSlave() != null) {
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
            MultiServerSocket.getNames().remove(0);
            setHasFinishedCurrentTrade(true);
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
