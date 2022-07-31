package scripts;

import org.jetbrains.annotations.NotNull;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.interfaces.Item;
import org.tribot.script.sdk.interfaces.Orientable;
import org.tribot.script.sdk.query.GameObjectQuery;
import org.tribot.script.sdk.query.InventoryQuery;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.script.ScriptConfig;
import org.tribot.script.sdk.script.TribotScript;
import org.tribot.script.sdk.script.TribotScriptManifest;
import org.tribot.script.sdk.types.*;
import org.tribot.script.sdk.walking.GlobalWalking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.Thread.sleep;

@TribotScriptManifest(name = "BasicWoodcutter", author = "Deniz", category = "Woodcutting", description = "Woodcutting bot")

public class WoodcuttingScript implements TribotScript {

    private final WorldTile lumbridge = new WorldTile(3230,3233,0);
    private final WorldTile draynor = new WorldTile(3085,3235,0);
    private final WorldTile GE = new WorldTile(3161,3486,0);
    private final WorldTile varrock = new WorldTile(3165, 3417, 0);
    private final List<String> logs = new ArrayList<>(Arrays.asList("logs", "Oak logs","Willow logs", "Yew logs"));

    private final ArrayList<Integer> freeWorlds = new ArrayList<>(Arrays.asList(301, 308, 316, 335, 371, 379, 380, 382, 384, 394, 397, 398, 399, 417, 418, 425, 426, 430, 431, 433, 434, 435, 436, 437, 451, 452, 453, 454, 455, 456, 469, 470 ,471, 472, 473, 474, 475, 476, 483, 497, 498, 499, 500, 501, 536, 537, 542, 543, 544, 545, 546, 547, 552, 553, 554, 555, 556, 557, 563, 564, 565, 566, 567, 571, 572, 573, 574, 575, 576));

    private String currentLogs = null;
    private WorldTile currentWorldTile = null;
    private String currentAxe = null;

    @Override
    public void configure(@NotNull ScriptConfig config) {
        config.setBreakHandlerEnabled(true);
        config.setRandomsAndLoginHandlerEnabled(true);
    }


    @Override
    public void execute(@NotNull String args) {
        init();
        BankManager.bank(currentAxe);
        CombatManager.killChickens();
        while(true) {
            init();
            if (MyPlayer.getRunEnergy() > 50 && !Options.isRunEnabled()) {
                Options.setRunEnabled(true);
            }

            if (Inventory.isFull()){
                // DROP OR BANK YOUR STUFF
                BankManager.bank(currentAxe);
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
                currentWorldTile = lumbridge; // edit this to lumby
                currentLogs = "tree";
                currentAxe = "Bronze axe";
            }else if (currentWCLevel > 15 && currentWCLevel <= 30) {
                currentWorldTile = varrock;
                currentLogs = "Oak logs";
                currentAxe = "Bronze axe";
            }else if (currentWCLevel >= 31 && currentWCLevel < 41) {
                currentLogs = "Willow logs";
                currentWorldTile = draynor;
                currentAxe = "Adamant axe";
            }else if (currentWCLevel >= 41) {
                currentAxe = "Rune axe";
                currentWorldTile = varrock;
                currentLogs = "Oak logs";
            } else{
                currentLogs = "Yew logs";
                currentWorldTile = GE;
                currentAxe = "Rune axe";
            }


    }

    public void ensureUsingRightAxe(String currentAxe, List<String> logs, WorldTile GE){
        if (!Query.inventory().nameEquals(currentAxe).isAny()) {
            GlobalWalking.walkToBank();
            Bank.open();
            Bank.depositEquipment();
            if (!Query.bank().nameEquals(currentAxe).isAny() && !Query.inventory().nameEquals(currentAxe).isAny()) {
                GrandExchangeManager.sellLogsIfPossible(logs, GE, currentAxe);
            }
            Bank.withdraw(currentAxe, 1);
            Bank.close();
        }
    }




    public void chopTree(){

        ensureUsingRightAxe(currentAxe, logs, GE);

        if (!currentWorldTile.isVisible() && !currentWorldTile.isRendered()) {
            GlobalWalking.walkTo(currentWorldTile);
        }

        if (Query.players().count() >= 7) {
            Collections.shuffle(freeWorlds);
            WorldHopper.hop(freeWorlds.get(10));
        }

        if (MyPlayer.getAnimation() == -1 && !MyPlayer.isMoving()){
            Query.gameObjects().nameEquals(currentLogs.split(" ")[0]).findClosestByPathDistance().map(tree -> tree.interact("Chop down"));
        }

    }

}