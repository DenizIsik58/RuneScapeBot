package scripts.rev;

import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Npc;

public class TargetManager {

    public static Npc chooseNewTarget(int monsterId){
        return Query.npcs().idEquals(monsterId)
                .isNotBeingInteractedWith()
                .isReachable()
                .isHealthBarNotVisible()
                .findBestInteractable().orElse(null);
    }

    public static boolean isTargetDead(Npc target){
        return !target.isValid() || target.getHealthBarPercent() == 0;
    }

}
