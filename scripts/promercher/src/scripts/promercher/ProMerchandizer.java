package scripts.promercher;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.Login;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.script.TribotScriptManifest;
import scripts.api.*;

import java.util.concurrent.atomic.AtomicReference;


@TribotScriptManifest(name = "ProMerchandizer", author = "Deniz", category = "Moneymaking", description = "This is an awesome mercher")
public class ProMerchandizer extends MyScriptExtension {
    public static AtomicReference<MercherState> state = new AtomicReference<>(MercherState.INIT);


    @Override
    protected void setupScript(ScriptSetup setup) {
//        we can add other options for script specific settings here.. for now this supports disabling the login handler and/or the break handler and main loop interval
//        like this:
        setup.setMainLoopInterval(100); //(its 50 by default)
        setup.disableWaitForLogin();
//        setup.disableBreakHandler();
        setup.disableLoginHandler();
    }

    @Override
    protected void onStart(String args) {
        //mulerWebhook = new MyDiscordWebhook("https://discord.com/api/webhooks/1010438256304861206/ZInuV2pDsUEAELlmxpeVYIIZo6F1MFSmCRacWIoEI16IYQIAKUXikPd40_K_kcNXCiNz");
        Log.debug("Script has been started");
        if (!Login.isLoggedIn()) {
            Login.login();
            Waiting.waitNormal(15000, 2000);
        }

    }

    @Override
    protected void onMainLoop() {
        MyScriptVariables.updateStatus(getState().toString());
        switch (getState()) {
            case INIT:
                handleInit();
                return;
            case MERCHING:
                handleMerhcing();
                return;
            case SELLING:
                handleSelling();
                return;
            case IDLING:
                handleIdling();
                //Login.logout();
                //scriptSetup.disableWaitForLogin();
                return;

        }
    }

    @Override
    protected void onEnding() {
        Log.debug("Ending script");

    }

    public static MercherState getState() {
        return state.get();
    }

    public static void setState(MercherState newState) {
        state.set(newState);
    }

    private void handleInit(){
        // Withdraw all our coins
        Waiting.waitUntil(2000, () -> MyBanker.withdraw("Coins", Integer.MAX_VALUE, false));
        Waiting.waitUntil(2000, MyBanker::closeBank);
        GrandExchangeManager.initiateGrandExchangeItemInfo();
        setState(MercherState.MERCHING);
    }

    private void handleIdling() {
        if (!MyExchange.hasOfferToCollect()) {
            Log.info("Waiting 30 seconds... No offers to collect");
            Waiting.wait(30040);
        }
        setState(MercherState.SELLING);
    }

    private void handleSelling(){
        MercherManager.startSelling();
        setState(MercherState.MERCHING);
    }

    private void handleMuling() {

    }

    private void handleMerhcing() {
        // When done merching
        // Set state to IDLING
        MercherManager.startMerching();
        setState(MercherState.IDLING);
    }

}
