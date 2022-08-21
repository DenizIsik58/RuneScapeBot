package scripts;

import org.jetbrains.annotations.NotNull;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.script.ScriptConfig;
import org.tribot.script.sdk.script.TribotScript;
import org.tribot.script.sdk.script.TribotScriptManifest;
import org.tribot.script.sdk.types.*;
import org.tribot.script.sdk.walking.GlobalWalking;
import scripts.api.MyCamera;
import scripts.api.MyOptions;
import scripts.api.MyWorldHopper;
import scripts.gui.GUI;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

@TribotScriptManifest(name = "BasicWoodcutter", author = "Deniz", category = "Woodcutting", description = "Woodcutting bot")

public class WoodcuttingScript implements TribotScript {

    private URL fxml;
    private WorldTile currentWorldTile = null;
    private String currentLogs = null;
    private int currentAxe = -1;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private static boolean bankLogs = false;
    private static boolean isGeChosenForYew = false;
    private static int hopAtX = 5;

    public static void setHopAtX(int hopAtX) {
        WoodcuttingScript.hopAtX = hopAtX;
    }

    public static void setIsGeChosenForYew(boolean isGeChosenForYew) {
        WoodcuttingScript.isGeChosenForYew = isGeChosenForYew;
    }

    public static boolean isIsGeChosenForYew() {
        return isGeChosenForYew;
    }

    public static boolean isBankLogs() {
        return bankLogs;
    }

    public static void setBankLogs(boolean bankLogs) {
        WoodcuttingScript.bankLogs = bankLogs;
    }

    @Override
    public void configure(@NotNull ScriptConfig config) {
        config.setBreakHandlerEnabled(true);
        config.setRandomsAndLoginHandlerEnabled(true);
    }

    @Override
    public void execute(@NotNull String args) {
        ScriptListening.addPreEndingListener(() -> {
            running.set(false);
        });

        while (GameState.getState() != GameState.State.LOGGED_IN) {
            Waiting.wait(250);
            Login.login();
        }

        try {
            File file = new File("/Users/deniz/Desktop/RuneScapeBot/scripts/woodcutter/src/scripts/gui.fxml");


            fxml = file.toURI().toURL();

        }catch (Exception e) {
            Log.error(e);
        }

        GUI gui = new GUI(fxml);
        gui.show();

        while (gui.isOpen()) {
            Waiting.wait(500);
        }

        init();
        MyCamera.init();
        MyOptions.init();
        CombatManager.killChickens();
        BankManager.bankLogs(currentAxe);

        while(running.get()) {
            init();

            if (Inventory.isFull()){
                // DROP OR BANK YOUR STUFF
                if (bankLogs) {
                    BankManager.bankLogs(currentAxe);
                } else {
                    BankManager.dropInventory(currentAxe);
                }
                //Inventory.drop(Inventory.getAll());
            }else {
                // START CHOPPING LOGS
                chopTree();
            }
            Waiting.wait(100);
        }
    }

    public void init(){
            var currentWCLevel = Skill.WOODCUTTING.getCurrentLevel();

            if (currentWCLevel < 15) {
                currentWorldTile = LocationManager.getLumbridge(); // edit this to lumby
                currentLogs = "tree";
                currentAxe = Axe.BRONZE.getAxeId();
            }else if (currentWCLevel > 15 && currentWCLevel <= 30) {
                currentWorldTile = LocationManager.getVarrock();
                currentLogs = "Oak logs";
                currentAxe = Axe.BRONZE.getAxeId();
            }else if (currentWCLevel >= 31 && currentWCLevel < 60) {
                currentLogs = "Willow logs";
                currentWorldTile = LocationManager.getDraynor();
                currentAxe = Axe.ADAMANT.getAxeId();
            }else{
                currentLogs = "Yew logs";
                currentWorldTile = isIsGeChosenForYew() ? LocationManager.getGE() : LocationManager.getEdgeville();
                currentAxe = Axe.RUNE.getAxeId();
            }
    }


    public void chopTree(){
        if (!currentWorldTile.isVisible() && !currentWorldTile.isRendered()) {
            GlobalWalking.walkTo(currentWorldTile);
        }

        // TODO: Put args to hop at X amount of people
        if (Query.players().count() >= hopAtX) {
            MyWorldHopper.hopToRandomFreeToPlayWorldWithRequirements();
        }

        if (isMyPlayerStandingStill()){
            getTree().ifPresent(tree -> tree.click("Chop"));
        }

    }

    private Optional<GameObject> getTree(){
        return Query.gameObjects()
                .nameEquals(currentLogs.split(" ")[0])
                .findClosestByPathDistance();
    }

    private boolean isMyPlayerStandingStill(){
        return MyPlayer.getAnimation() == -1 && !MyPlayer.isMoving();
    }

}