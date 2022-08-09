package scripts.rev;

import dax.api_lib.DaxWalker;
import dax.walker_engine.WalkingCondition;
import lombok.Getter;
import org.tribot.api.util.Screenshots;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.input.Mouse;
import org.tribot.script.sdk.painting.template.basic.BasicPaintTemplate;
import org.tribot.script.sdk.pricing.Pricing;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.script.TribotScriptManifest;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import org.tribot.util.Util;
import scripts.api.*;
import scripts.api.concurrency.Debounce;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static dax.shared.helpers.BankHelper.openBank;


@TribotScriptManifest(name = "Revs", author = "Deniz", category = "Moneymaking")
public class RevScript extends MyScriptExtension {

    @Getter
    private DetectPlayerThread playerDetectionThread = null;

    private MulingClient muleClient;
    public AtomicReference<State> state = new AtomicReference<>(State.STARTING);
    private WorldTile selectedMonsterTile = new WorldTile(3160, 10115,0 ); // West demons by default
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean inWilderness = new AtomicBoolean(false);

    private final Debounce walkDebounce = new Debounce(1000, TimeUnit.MILLISECONDS);
    private DiscordWebhook lootWebhook;
    private DiscordWebhook onEndWebHook;

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
    protected void setupPaint(BasicPaintTemplate.BasicPaintTemplateBuilder paint) {
        paint
                .row(getTextRowTemplate()
                    .label("PKER DETECTED")
                    .condition(() -> playerDetectionThread != null && playerDetectionThread.hasPkerBeenDetected())
                    .build())
                .row(getTextRowTemplate()
                    .label("DETECTING")
                    .condition(() -> playerDetectionThread != null && playerDetectionThread.isRunning())
                    .build());

    }

    @Override
    protected void onStart(String args) throws IOException {
        // we put the args from the script start here so incase you have a script with args you can use them in your script from this
        lootWebhook = new DiscordWebhook("https://discord.com/api/webhooks/1006526256378040390/lBQqh9sKBdmHY3DFI7gKBhAq38gMZr5SsC8CUTICxqYLfrivwA4YI_ODE8iZFjRDuEwm");
        onEndWebHook = new DiscordWebhook("https://discord.com/api/webhooks/1006528403580649564/bTiJDmc9LL-XPRMViwi8I5qkOnPlDdfQK9m-VV3FReGvCTh_F8IKYXFYJ8uuJPKDfOI4");

        MessageListening.addServerMessageListener(MyRevsClient::processMessage);

        DaxWalker.setGlobalWalkingCondition(() -> {
            handlePkThread();
            if (isCancellingWalking()) {
                // if we shouldn't walk, and since we are here we are walking,
                // this debounce will extend the timer for when walking should be cancelled
                walkDebounce.debounce();
                return WalkingCondition.State.EXIT_OUT_WALKER_FAIL;
            }
            return WalkingCondition.State.CONTINUE_WALKER;
        });

        try {
            muleClient = new MulingClient();
            muleClient.startConnection("127.0.0.1", 6668);
        }catch (Exception e){
            Log.debug(e);
        }


        MyOptions.init();
        MyCamera.init();
        PrayerManager.init();

    }


    @Override
    protected void onMainLoop() {
        MyScriptVariables.updateStatus(state.toString());
        updateState();

        handlePkThread();

        if (playerDetectionThread != null && playerDetectionThread.hasPkerBeenDetected()){
            //Log.info("Pker detected.");
            return;
        }


        Mouse.setSpeed(200);

        MyOptions.setRunOn();

        switch(getState()) {
            case STARTING:
                handleStarting();
                return;
            case BANKING:
                handleBanking();
                return;
            case WALKING:
                handleWalking();
                return;
            case SELLLOOT:
                handleSellLoot();
                return;
            case KILLING:
                handleKilling();
                return;
            case DEATH:
                handleDeath();
                return;
            case LOOTING:
                handleLooting();
                return;
        }
    }

    @Override
    protected void onEnding() {
        // Send a SS to discord
        var outputFile = ScreenShotManager.takeScreenShotAndSave();

        MyRevsClient.getScript().getLootWebhook().setUsername("Revenant Farm")
                .setContent("Reventant script ended! User " + MyPlayer.getUsername() + " managed to farm a total of **" + LootingManager.getTotalValue() + " Gold**")
                .addFile(outputFile)
                .execute();

        if (muleClient != null) {
            muleClient.stopConnection();
        }
        if (Combat.isInWilderness()){
            TeleportManager.teleportOutOfWilderness("Ending script... Teleporting out");
        }
    }

    private void updateState() {
        if (Query.npcs().nameEquals("Death").isAny()){
            Query.gameObjects().idEquals(39549).findFirst().ifPresent(portal -> portal.click("Use"));
        }

        if (isState(State.STARTING) || isState(State.SELLLOOT)) {
            return;
        }
        if (MyRevsClient.myPlayerIsInGE() && !isState(State.BANKING))  {
            setState(State.BANKING);
            return;
        }

        if (MyRevsClient.myPlayerIsDead()){
            if (playerDetectionThread != null){
                playerDetectionThread.setHasPkerBeenDetected(false);
            }
            TeleportManager.setHasVisitedBeforeTrip(false);
            setState(State.DEATH);
            return;
        }
        if (MyRevsClient.myPlayerIsInFerox()) {
            // if not bank task is satisfied
            // teleport to ge
            // else
            if (MyRevsClient.myPlayerIsInFerox()){
                GlobalWalking.walkTo(new WorldTile(3133, 3628, 0)); // bank spot at ferox
            }

            if (!BankManagerRevenant.isEquipmentBankTaskSatisfied() && !BankManagerRevenant.isInventoryBankTaskSatisfied()){
                openBank();
                BankManagerRevenant.checkIfNeedToBuyGear();
                BankManagerRevenant.checkIfNeedToRestockSupplies();
                BankManagerRevenant.getEquipmentBankTask().execute();
                BankManagerRevenant.getInventoryBankTask().execute();
            }

            setState(State.WALKING);
            return;
        }
    }

    private void killPkThread() {
        if (playerDetectionThread != null) {
            if (playerDetectionThread.isRunning()) playerDetectionThread.stopDetection();
            playerDetectionThread = null;
        }
    }

    private void startPkThread() {
        if (playerDetectionThread != null) killPkThread();
        playerDetectionThread = new DetectPlayerThread(this);
        playerDetectionThread.start();
    }

    public boolean isCancellingWalking() {
        return walkDebounce.isDebounced();
    }

    public void cancelWalking() {
        walkDebounce.debounce();
    }

    private void handlePkThread() {
        var wasInWild = inWilderness.get();
        var isInWild = Combat.isInWilderness();

        if (wasInWild != isInWild) {
            if (isInWild) startPkThread();
            else killPkThread();
            inWilderness.set(isInWild);
        }
    }

    private void handleSellLoot() {

        GrandExchangeRevManager.sellLoot();
        //Log.warn("NOT YET IMPLEMENTED");
    }

    private void handleStarting() {
        if (!MyRevsClient.myPlayerIsInFerox()){
            MyExchange.walkToGrandExchange();
            var isInGe = Waiting.waitUntil(MyRevsClient::myPlayerIsInGE);
            if (!isInGe){
                Log.debug("Couldn't get to grand exchange");
                return;
            }
            Log.debug("I'm in GE");
        }
        BankManagerRevenant.init();
    }

    private void handleWalking() {
        var tile = TeleportManager.refill();
        selectedMonsterTile = tile;
        if (TeleportManager.monsterTileIsDetected(tile)) {
            Log.debug("I have detected the monster place");
            MyOptions.init();
            MyCamera.init();
            PrayerManager.init();
            RevkillerManager.setiWasFirst(false);
            Log.debug("Switching to killing state");
            setState(State.KILLING);
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
        LootingManager.resetTripValue();
        if (playerDetectionThread != null){
            playerDetectionThread.setHasPkerBeenDetected(false);
        }
        TeleportManager.setHasVisitedBeforeTrip(false);
        BankManagerRevenant.bankLoot();
    }

    private void handleLooting() {
        LootingManager.loot();
    }

    private void handleDeath() {
        TeleportManager.setHasVisitedBeforeTrip(false);
        DeathManger.incrementTotalDeaths();
        Log.info("Oh dear! FUCK! :( - You have just died with: " + LootingManager.getTripValue() + " Gold!! BASTARD");
        // in the future we should implement logging the pker names... for hate and for lookout lol
        Log.info("Total times died so far: " + DeathManger.totalDeaths());
        MyScriptVariables.setDeath(String.valueOf(DeathManger.totalDeaths()));
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
        if (muleClient != null) {
            return muleClient;
        }
        muleClient = new MulingClient();
        muleClient.startConnection("localhost", 6668);
        return muleClient;
    }

    public DiscordWebhook getLootWebhook() {
        return lootWebhook;
    }

    public DiscordWebhook getOnEndWebHook() {
        return onEndWebHook;
    }

    public WorldTile getSelectedMonsterTile() {
        return selectedMonsterTile;
    }
}
