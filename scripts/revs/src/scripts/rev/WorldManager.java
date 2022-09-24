package scripts.rev;

import org.tribot.script.sdk.WorldHopper;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.World;

public class WorldManager {
    public static boolean hopToRandomMemberWorldWithRequirements(){
        return Query
                .worlds()
                .isMembers()
                .isNotDangerous()
                .isRequirementsMet()
                .isNotAnyType(World.Type.PVP_ARENA,
                        World.Type.BOUNTY,
                        World.Type.TOURNAMENT,
                        World.Type.HIGH_RISK,
                        World.Type.LAST_MAN_STANDING,
                        World.Type.LEAGUE,
                        World.Type.PVP,
                        World.Type.DEADMAN,
                        World.Type.DEADMAN_TOURNAMENT)
                .worldNumberNotEquals(403, 404, 407, 408, 411, 412)
                .isNotCurrentWorld()
                .findRandom()
                .map(i -> WorldHopper.hop(i.getWorldNumber()))
                .orElse(false);
    }
}
