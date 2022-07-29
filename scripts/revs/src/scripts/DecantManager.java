package scripts;

import org.tribot.script.sdk.Bank;
import org.tribot.script.sdk.BankSettings;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DecantManager {
    public static boolean hasDecanted = false;
    private static final List<String> potions = new ArrayList<>(Arrays.asList("Divine ranging potion(", "Stamina potion(", "Prayer potion("));


    public static void decantPotionsFromBank(){
        if(!BankSettings.isNoteEnabled()){
            BankSettings.setNoteEnabled(true);
        }

        for (var item: potions){
            var noted = Query.bank().nameContains(item).findFirst().orElse(null);
            if (noted != null){
                Bank.withdrawAll(item);
                Waiting.wait(3000);
            }
        }

        GlobalWalking.walkTo(new WorldTile(3157, 3481, 0));
        Query.npcs().idEquals(5449).findBestInteractable().map(c -> c.interact("Decant"));
        hasDecanted = true;
    }
}
