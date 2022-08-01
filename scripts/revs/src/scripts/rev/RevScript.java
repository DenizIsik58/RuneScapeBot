package scripts.rev;

import org.tribot.script.sdk.Combat;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.MessageListening;
import org.tribot.script.sdk.script.TribotScriptManifest;
import org.tribot.script.sdk.types.WorldTile;
import scripts.api.MyCamera;
import scripts.api.MyExchange;
import scripts.api.MyOptions;
import scripts.api.MyScriptExtension;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static dax.shared.helpers.BankHelper.openBank;

@TribotScriptManifest(name = "Revs", author = "Deniz", category = "Moneymaking")
public class RevScript extends MyScriptExtension {

    private DetectPlayerThread playerDetectionThread = null;
    private MulingClient muleClient;
    public AtomicReference<State> state = new AtomicReference<>(State.STARTING);
    private WorldTile selectedMonsterTile = new WorldTile(3160, 10115,0 ); // West demons by default
    private final AtomicBoolean running = new AtomicBoolean(true);



    @Override
    protected void setupScript(ScriptSetup setup) {
//        we can add other options for script specific settings here.. for now this supports disabling the login handler and/or the break handler and main loop interval
//        like this:
//        setup.setMainLoopInterval(100); (its 50 by default)
//        setup.disableWaitForLogin();
//        setup.disableBreakHandler();
//        setup.disableLoginHandler();
    }

    @Override
    protected void onStart(String args) {
        // we put the args from the script start here so incase you have a script with args you can use them in your script from this

        MessageListening.addServerMessageListener(MyRevsClient::processMessage);

        muleClient = new MulingClient();
        muleClient.startConnection("127.0.0.1", 6668);

        playerDetectionThread = new DetectPlayerThread(running::get);
        playerDetectionThread.start();
        PrayerManager.init();

    }


    @Override
    protected void onMainLoop() {

        updateState();
        if (playerDetectionThread.hasPkerBeenDetected()){
            Log.info("Pker detected.");
            return;
        }

        MyOptions.setRunOn();

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

    @Override
    protected void onEnding() {
        if (muleClient != null) muleClient.stopConnection();
        if (Combat.isInWilderness()){
            TeleportManager.teleportOutOfWilderness("Ending script... Teleporting out");
        }
    }

    private void updateState() {
        if (isState(State.STARTING)) return;
        if (MyRevsClient.myPlayerIsInGE() && !isState(State.BANKING))  setState(State.BANKING);
        if (MyRevsClient.myPlayerIsDead()){
            playerDetectionThread.setHasPkerBeenDetected(false);
            setState(State.DEATH);
        }
        if (MyRevsClient.myPlayerIsInFerox()) {
            // if not bank task is satisfied
            // teleport to ge
            // else
            if (!BankManagerRevenant.isEquipmentBankTaskSatisfied() && !BankManagerRevenant.isInventoryBankTaskSatisfied()){
                openBank();
                BankManagerRevenant.checkIfNeedToBuyGear();
                BankManagerRevenant.checkIfNeedToRestockSupplies();
                BankManagerRevenant.getEquipmentBankTask().execute();
                BankManagerRevenant.getInventoryBankTask().execute();

            }
            setState(State.WALKING);
        }
    }

    private void handleSellLoot() {
        Log.warn("NOT YET IMPLEMENTED");
    }

    private void handleStarting() {
        MyExchange.walkToGrandExchange();
        BankManagerRevenant.init();
        setState(State.WALKING);
        Log.debug(state);
    }

    private void handleWalking() {
        var tile = TeleportManager.refill();
        selectedMonsterTile = tile;
        if (TeleportManager.monsterTileIsDetected(tile)) {
            MyOptions.init();
            MyCamera.init();
            PrayerManager.init();
            LootingManager.resetTripValue();
            RevkillerManager.setiWasFirst(false);
            BoostingManager.resetBoost();
            setState(State.KILLING);
            Log.debug(state);
        }
    }

    private void handleKilling() {
        RevkillerManager.killMonster();
    }

    private void handleBanking() {
        Log.debug(state);
        Log.info("Total amount made this trip: " + LootingManager.getTripValue());
        Log.info("Total amount made since script start: " + LootingManager.getTotalValue());
        Log.info("Total times died so far: " + DeathManger.totalDeaths());
        playerDetectionThread.setHasPkerBeenDetected(false);
        TeleportManager.setHasVisitedBeforeTrip(false);
        BankManagerRevenant.bankLoot();
    }

    private void handleLooting() {
        LootingManager.loot();

    }

    private void handleDeath() {
        Log.debug(state);
        TeleportManager.setHasVisitedBeforeTrip(false);
        DeathManger.incrementTotalDeaths();
        Log.info("Oh dear! FUCK! :( - You have just died with: " + LootingManager.getTripValue() + " Gold!! BASTARD");
        // in the future we should implement logging the pker names... for hate and for lookout lol
        Log.info("Total times died so far: " + DeathManger.totalDeaths());
        LootingManager.setTotalValue(LootingManager.getTotalValue() - LootingManager.getTripValue());
        LootingManager.resetTripValue();
        DeathManger.reGearFromDeath();
    }

    public State getState() {
        return state.get();
    }

    public void setState(State state) {
        this.state.set(state);
    }

    public boolean isState(State state) {
        return getState().equals(state);
    }

    public MulingClient getSocketClient() {
        return muleClient;
    }

    public WorldTile getSelectedMonsterTile() {
        return selectedMonsterTile;
    }
}