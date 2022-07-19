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
        CombatManager.killChickens();
        while(true) {

            if (MyPlayer.getRunEnergy() > 50 && !Options.isRunEnabled()) {
                Options.setRunEnabled(true);
            }

            if (MyPlayer.isHealthBarVisible() && MyPlayer.get().get().isInteractingWithMe()) {
                GlobalWalking.walkToBank();
            }
            if (Inventory.isFull()){
                // DROP OR BANK YOUR STUFF
                bank();
                //Inventory.drop(Inventory.getAll());
            }else {
                // START CHOPPING LOGS
                chopTree();
            }
        }
    }

    public void init(){
            var currentWCLevel = Skill.WOODCUTTING.getCurrentLevel();
        Log.info("initializing...");
        Log.info("Woodcutting level: " + currentWCLevel);
            if (currentWCLevel < 15) {
                currentWorldTile = lumbridge; // edit this to lumby
                currentLogs = "logs";
                currentAxe = "Bronze axe";
            }else if (currentWCLevel > 15 && currentWCLevel < 30) {
                Log.info("HITTT");
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


    public void bank() {
        if (!Bank.isNearby()){
            GlobalWalking.walkToBank();
        }else {
            if (!Bank.isOpen()){
                Bank.open();
            }
                Bank.depositInventory();
                Bank.withdraw(currentAxe, 1);
                Bank.close();

        }
    }

    public void sellLogsIfPossible(){

                var amountOfLogs = logs.stream().mapToInt(Bank::getCount).sum();
                Log.info(amountOfLogs);
                if (amountOfLogs >= 400) {
                    Log.info("Have more than 400 logs");
                    BankSettings.setNoteEnabled(true);
                    Log.info("Withdrawing logs");
                    logs.forEach(Bank::withdrawAll);
                    BankSettings.setNoteEnabled(false);
                    Bank.close();

                if (!GrandExchange.isNearby()) {
                    GlobalWalking.walkTo(GE);
                }
                if (!GrandExchange.isOpen()) {
                    GrandExchange.open();
                    var inventory = Inventory.getAll();
                    for (var item :
                            inventory) {
                        if (item.getName().contains("logs")) {
                            logs.forEach(log -> GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().itemName(log).quantity(Inventory.getCount(item.getName())).priceAdjustment(-5).type(GrandExchangeOffer.Type.SELL).build()));
                        }
                    }
                    GrandExchange.collectAll();
                    GrandExchange.close();
                    if (!Bank.contains(currentAxe) && !Inventory.contains(currentAxe) && !MyPlayer.get().flatMap(player -> player.getEquippedItem(Equipment.Slot.WEAPON)).get().getName().equals(currentAxe)){
                        buyPrefferedAxe();
                        GrandExchange.collectAll();
                        GrandExchange.close();
                    }
                }
        }
    }

    public void buyPrefferedAxe(){
        GrandExchange.placeOffer(GrandExchange.CreateOfferConfig.builder().searchText(currentAxe).type(GrandExchangeOffer.Type.BUY).priceAdjustment(3).build());
    }


    public void chopTree(){

        if (!Query.inventory().nameContains(currentAxe).isAny()) {
            GlobalWalking.walkToBank();
            Bank.open();
            Bank.depositEquipment();
            if (!Query.bank().nameEquals(currentAxe).isAny() && !Query.inventory().nameEquals(currentAxe).isAny()) {
                sellLogsIfPossible();
            }
            Bank.withdraw(currentAxe, 1);
            Bank.close();
        }


        if (!currentWorldTile.isVisible()) {
            GlobalWalking.walkTo(currentWorldTile);
        }

        if (Query.players().count() >= 7) {
            Collections.shuffle(freeWorlds);
            WorldHopper.hop(freeWorlds.get(10));
        }

        if (MyPlayer.getAnimation() == -1 && !MyPlayer.isMoving()){
            Query.gameObjects().nameEquals(currentLogs.split(" ")[0]).findClosestByPathDistance().get().click("Chop down");
        }

    }

}