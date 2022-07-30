package scripts;

import lombok.SneakyThrows;

import org.jetbrains.annotations.NotNull;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.script.ScriptConfig;
import org.tribot.script.sdk.script.TribotScript;
import org.tribot.script.sdk.script.TribotScriptManifest;

import java.io.IOException;
import java.net.Socket;

@TribotScriptManifest(name = "RevMuler", author = "Deniz", category = "Tools", description = "My muler")


public class MulerScript implements TribotScript {
    private static MulerState state;
    private MultiServerSocket muler;

    public static void processTrade(String name){

        while(MultiServerSocket.getNames().size() != 0){
            var player = "";
            //Log.info("Got a muling request by from " + name);
            for(var playerName: MultiServerSocket.getNames()){
                if (playerName.equals(name)){
                    player = playerName;
                    Chatbox.acceptTradeRequest(name);
                    Waiting.waitUntil(() -> TradeScreen.OtherPlayer.contains("Coins"));

                    // First trade
                    Waiting.waitUntil(TradeScreen.OtherPlayer::hasAccepted);
                    TradeScreen.accept();

                    // Second trade
                    Waiting.waitUntil(TradeScreen.OtherPlayer::hasAccepted);
                    MultiServerSocket.getNames().remove(player);
                }
            }
        }
        state = MulerState.IDLING;
    }

    @Override
    public void configure(@NotNull ScriptConfig config) {
        //config.setBreakHandlerEnabled(true);
        //config.setRandomsAndLoginHandlerEnabled(true);
    }

    @SneakyThrows
    @Override
    public void execute(@NotNull String args) {
        Log.info("Script has been started");


        MessageListening.addTradeRequestListener(MulerScript::processTrade);
        ScriptListening.addPreEndingListener(() -> {
            try {
                muler.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Log.info("Server initiated");
        state = MulerState.IDLING;
        muler = new MultiServerSocket();
        new Thread(muler).start();

        while (true){
            //Log.info("I'm here");

            if (state == MulerState.IDLING){
                //Log.info(state);
                if (Login.isLoggedIn()){
                    //Log.info(Login.isLoggedIn());
                    //Log.info("Im logged in");
                    Login.logout();
                    Waiting.wait(5000);
                }
            }

            if (state == MulerState.MULING){
                //Log.info(state);
                //Log.info(Login.isLoggedIn());
                if (!Login.isLoggedIn()){
                    Login.login();
                }
            }
            Waiting.wait(100);
        }


    }

    public static MulerState getState() {
        return state;
    }

    public static void setState(MulerState state) {
        MulerScript.state = state;
    }
}
