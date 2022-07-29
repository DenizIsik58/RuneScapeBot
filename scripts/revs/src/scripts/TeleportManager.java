package scripts;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Area;
import org.tribot.script.sdk.types.InventoryItem;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.GlobalWalking;
import org.tribot.script.sdk.walking.WalkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.tribot.script.sdk.Waiting.*;

public class TeleportManager {

    private static final WorldTile enclaveDoor = new WorldTile(3123, 3628, 0);
    private static final WorldTile caveEntrance = new WorldTile(3075, 3652, 0);
    private static final WorldTile enclavePool = new WorldTile(3128, 3634, 0);
    private static final WorldTile south_ork = new WorldTile(3216, 10091, 0);
    private static final WorldTile north_ork = new WorldTile(3226, 10132,0 );
    public static final WorldTile demons = new WorldTile(3160, 10115,0 );


    public static WorldTile refillAndWalkToCave(){
        var ring = Query.inventory().nameContains("Ring of dueling").findFirst().orElse(null);
        if (ring != null){
            if (!Area.fromRectangle(new WorldTile(3153,3638, 0), new WorldTile(3126, 3618, 0)).contains(MyPlayer.getTile())) {
                ring.click("Rub");
                Waiting.waitUntil(2000, () -> ChatScreen.containsOption("Ferox Enclave."));
                ChatScreen.selectOption("Ferox enclave.");
                Waiting.wait(6000);
            }
        }else {
                BankManagerRevenant.withdrawItemByName("Ring of dueling");
                Bank.close();
                Waiting.wait(4000);
        }
        List<WorldTile> monsterTiles = new ArrayList<>(Arrays.asList(south_ork, north_ork, demons));
        Collections.shuffle(monsterTiles);
        var randomMonsterTile = monsterTiles.get(0);

        if (Area.fromRectangle(new WorldTile(3153,3638, 0), new WorldTile(3126, 3618, 0)).contains(MyPlayer.getTile())) {
            GlobalWalking.walkTo(enclavePool);
            var pool = Query.gameObjects().idEquals(39651).findClosest().orElse(null);
            if (pool != null) {
                if (pool.isVisible()) {
                    pool.interact("Drink");
                    waitUntil(10000, () -> MyPlayer.getAnimation() == 7305);
                }

            }
            GlobalWalking.walkTo(enclaveDoor); // Enclave door

            var enclaveDoor = Query.gameObjects().idEquals(39656).findBestInteractable().orElse(null);
            if (enclaveDoor != null) {
                if (enclaveDoor.isVisible()) {
                    enclaveDoor.click("Pass-Through");
                    Waiting.waitUntilInArea(Area.fromRectangle(new WorldTile(3122, 3629, 0), new WorldTile(3121, 3628, 0)), 1000);
                }

            }
            GlobalWalking.walkTo(caveEntrance, TeleportManager::processWalking); // Cave entrance
            Query.gameObjects().idEquals(31555).findBestInteractable().map(c -> c.interact("Enter"));
            Waiting.waitUntilInArea(Area.fromRectangle(new WorldTile(3192, 10062, 0), new WorldTile(3202, 10051, 0)), 10000);

            if (!MyPlayer.isStaminaActive()) {
                Query.inventory().nameContains("Stamina potion").findFirst().map(InventoryItem::click);
            }
            GlobalWalking.walkTo(randomMonsterTile, TeleportManager::processWalking);

        }
        return randomMonsterTile;
    }

    public static int getMonsterIdBasedOnLocation(WorldTile tile){
        if (tile.getX() != demons.getX()){
            return 7937;
        }
            return 7936;
    }

    public static WalkState processWalking(){
        if (!GameTab.LOGOUT.isOpen()){
            GameTab.LOGOUT.open();
        }

        if (PkerDetecter.isPkerDetected() && !MyRevsClient.myPlayerIsInFerox()){
            if (!Query.players().isInteractingWithMe().isAny()){
                WorldManager.hopToRandomMemberWorldWithRequirements();
                return WalkState.CONTINUE;
            }else {
                PkerDetecter.quickTele();
                return WalkState.FAILURE;
            }
        }
        return WalkState.CONTINUE;
    }
}
