package scripts.rev;

import dax.api_lib.DaxWalker;
import dax.walker_engine.WalkingCondition;
import lombok.Getter;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.input.Mouse;
import org.tribot.script.sdk.painting.template.basic.BasicPaintTemplate;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.script.TribotScriptManifest;
import org.tribot.script.sdk.types.GameObject;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import scripts.api.*;
import scripts.api.concurrency.Debounce;
import scripts.api.gui.MyGUI;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
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
    private WorldTile selectedMonsterTile = new WorldTile(3216, 10091, 0); // South ork by default
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean inWilderness = new AtomicBoolean(false);
    private AtomicBoolean skulledScript = new AtomicBoolean(true);
    private final Debounce walkDebounce = new Debounce(1000, TimeUnit.MILLISECONDS);
    private DiscordWebhook lootWebhook;
    private DiscordWebhook onEndWebhook;
    private DiscordWebhook onDeathWebhook;
    private DiscordWebhook successfullTripHook;
    private URL fxml;

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
        onEndWebhook = new DiscordWebhook("https://discord.com/api/webhooks/1006528403580649564/bTiJDmc9LL-XPRMViwi8I5qkOnPlDdfQK9m-VV3FReGvCTh_F8IKYXFYJ8uuJPKDfOI4");
        onDeathWebhook = new DiscordWebhook("https://discord.com/api/webhooks/1006886106870075443/KgnJFpyL07_92FZ2fk8pxpCSDCxDQ_pIDDU0i2NwhxvRFG8KScu1eLKMz9VfT1xcwI3N");
        successfullTripHook = new DiscordWebhook("https://discord.com/api/webhooks/1007597804274847824/0BeuF_rHMu3N1Gqa0Lm1teNGl2-KSDSfCu7A4GmyZIlSx6x0I5KlXAvfnO8UNFSirN5V");

       /* try {
            File file = new File("C:\\Users\\Administrator\\Documents\\GitHub\\RuneScapeBot\\scripts\\revs\\src\\scripts\\rev\\gui.fxml");


            fxml = file.toURI().toURL();

        }catch (Exception e) {
            Log.error(e);
        }

        MyGUI gui = new MyGUI(fxml);
        gui.show();

        while (gui.isOpen()) {
            Waiting.wait(500);
        }
*/
        /*
        GameListening.addGameTickListener(new Runnable() {
            @Override
            public void run() {
                Log.debug("Tick");
                if (DetectPlayerThread.hasTickCounterStarted()) {
                    Log.debug("Ticking: " + DetectPlayerThread.tickCounter());
                    DetectPlayerThread.setTickCounter(DetectPlayerThread.tickCounter() + 1);
                    if (DetectPlayerThread.tickCounter() == 3) {
                        Log.debug("3 ticks teleporting now");
                        Equipment.Slot.RING.getItem().map(c -> c.click("Grand Exchange"));
                    }

                }
            }
        });*/


        if (MyClient.findTRiBotFrame() != null){
            MyClient.findTRiBotFrame().setState(JFrame.ICONIFIED);
        }

        MessageListening.addServerMessageListener(MyRevsClient::processMessage);

        /*DaxWalker.setGlobalWalkingCondition(() -> {
            handlePkThread();
            if (isCancellingWalking()) {
                // if we shouldn't walk, and since we are here we are walking,
                // this debounce will extend the timer for when walking should be cancelled
                walkDebounce.debounce();
                return WalkingCondition.State.EXIT_OUT_WALKER_FAIL;
            }
            return WalkingCondition.State.CONTINUE_WALKER;
        });*/

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


        Mouse.setSpeed(300);

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
        var outputFile = ScreenShotManager.takeScreenShotAndSave("onend");

        getOnEndWebhook().setUsername("Revenant Farm")
                .setContent("Reventant script ended! User " + MyPlayer.getUsername() + " managed to farm a total of **" + LootingManager.getTotalValue() + " Gold**")
                .addFile(outputFile)
                .execute();

        if (muleClient != null) {
            muleClient.stopConnection();
        }
        if (Combat.isInWilderness()){
            TeleportManager.teleportOutOfWilderness("Ending script... Teleporting out");
        }

        Objects.requireNonNull(MyClient.findTRiBotFrame()).setState(Frame.NORMAL);
    }

    private void updateState() {
        if (Query.npcs().nameEquals("Death").isAny()){
            Query.gameObjects().idEquals(39549).findFirst().ifPresent(portal -> portal.click("Use"));
        }

        if (MyPlayer.getTile().getPlane() == 1) {
            Query.gameObjects().idEquals(14746, 39650, 39643 ).findClosest().map(GameObject::click);
        }

        if (Query.worlds().worldNumberEquals(WorldHopper.getCurrentWorld()).isNonMembers().isAny()) {
            Log.debug("We are not in a members world. Hopping");
            WorldManager.hopToRandomMemberWorldWithRequirements();
        }

        if (isState(State.STARTING) || isState(State.SELLLOOT)) {
            return;
        }
        if (MyRevsClient.myPlayerIsInGE() && !isState(State.BANKING))  {
            setState(State.BANKING);
        }

        if (!Combat.isInWilderness()) {
            if (playerDetectionThread != null){
                playerDetectionThread.setHasPkerBeenDetected(false);
            }
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
            GlobalWalking.walkTo(new WorldTile(3133, 3628, 0)); // bank spot at ferox

            if (!BankManagerRevenant.isEquipmentBankTaskSatisfied() && !BankManagerRevenant.isInventoryBankTaskSatisfied()){
                openBank();
                BankManagerRevenant.checkIfNeedToBuyGear();
                BankManagerRevenant.checkIfNeedToRestockSupplies();
                BankManagerRevenant.getEquipmentBankTask().execute();
                BankManagerRevenant.getInventoryBankTask().execute();
                BankManagerRevenant.wearAvarice();
            }
        }
    }

    public void killPkThread() {
        if (playerDetectionThread != null) {
            if (playerDetectionThread.isRunning()){
                playerDetectionThread.stopDetection();
            }
            playerDetectionThread = null;
        }
    }

    private void startPkThread() {
        if (playerDetectionThread != null) {
            killPkThread();
        }
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
        //Log.debug("Handling pk thread");
        var wasInWild = inWilderness.get();
        var isInWild = Combat.isInWilderness();
        if (wasInWild != isInWild) {
            if (isInWild) {
                startPkThread();
            } else {
                killPkThread();
            }
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
        selectedMonsterTile = TeleportManager.refill();
        if (TeleportManager.monsterTileIsDetected(selectedMonsterTile)) {//
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
        PrayerManager.turnOffAllPrayer();

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
        Log.debug("Dead");

        MyPlayer.getTile().translate(5,1).click();
        Waiting.wait(2000);
        PrayerManager.turnOffAllPrayer();
        TeleportManager.setHasVisitedBeforeTrip(false);
        DeathManger.incrementTotalDeaths();
        LootingManager.setTotalValue(LootingManager.getTotalValue() - LootingManager.getTripValue() - (MyRevsClient.getScript().isSkulledScript() ? 200000 :500000));
        try {
            var outputFile = ScreenShotManager.takeScreenShotAndSave("ondeath");

            onDeathWebhook.setUsername("Revenant Farm")
                    .setContent("**" + MyPlayer.getUsername() + "** has just died with: " + LootingManager.getTripValue() + " Gold - profit so far: **" + LootingManager.getTotalValue() + "** - Total times dies: " + DeathManger.totalDeaths())
                    .addFile(outputFile)
                    .execute();
        }catch (Exception e) {
            Log.error(e);
        }
        // in the future we should implement logging the pker names... for hate and for lookout lol
        MyScriptVariables.setDeath(String.valueOf(DeathManger.totalDeaths()));
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

    public void setSkulledScript(boolean skulledScript) {
        this.skulledScript.set(skulledScript);
    }

    public boolean isSkulledScript() {
        return skulledScript.get();
    }

    public DiscordWebhook getLootWebhook() {
        return lootWebhook;
    }

    public DiscordWebhook getOnEndWebhook() {
        return onEndWebhook;
    }

    public DiscordWebhook getSuccessfullTripHook() {
        return successfullTripHook;
    }

    public WorldTile getSelectedMonsterTile() {
        return selectedMonsterTile;
    }
}
