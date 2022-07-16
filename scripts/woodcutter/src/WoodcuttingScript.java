import org.jetbrains.annotations.NotNull;
import org.tribot.script.sdk.Bank;
import org.tribot.script.sdk.Equipment;
import org.tribot.script.sdk.Inventory;
import org.tribot.script.sdk.MyPlayer;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.script.ScriptConfig;
import org.tribot.script.sdk.script.TribotScript;
import org.tribot.script.sdk.script.TribotScriptManifest;
import org.tribot.script.sdk.walking.GlobalWalking;

@TribotScriptManifest(name = "MyScript", author = "Scripter", category = "Template", description = "This is a template")

public class WoodcuttingScript implements TribotScript {


    @Override
    public void configure(@NotNull ScriptConfig config) {
        config.setBreakHandlerEnabled(true);
        config.setRandomsAndLoginHandlerEnabled(true);
    }

    @Override
    public void execute(@NotNull String args) {
        var player = MyPlayer.get().get();
        while(true) {
            if (player.getEquippedItem(Equipment.Slot.WEAPON).isPresent()) {
                wearAxe(1351);
            }
            if (Inventory.isFull()){
                // DROP OR BANK YOUR STUFF
                bank();
            }else {
                // START CHOPPING LOGS
                chopTree();
            }
        }
    }


    public void bank() {
        if (!Bank.isNearby()){
            GlobalWalking.walkToBank();
        }else {
            if (!Bank.isOpen()){
                Bank.open();
            }else {
                Bank.depositInventory();
            }
        }
    }

    public void chopTree(){
        var objects = Query.gameObjects().toList();
        for (var obj : objects) {
            if (obj.getName().equals("Tree")){
                obj.click("Chop");
            }
        }



    }

    public void wearAxe(int axeId){
        var invy = Inventory.getAll();
        for (int i = 0; i < invy.size() -1 ; i++){
            if (invy.get(i).getId() == axeId) {
                invy.get(i).hover();
                invy.get(i).click("Wear");
            }
        }
    }

}