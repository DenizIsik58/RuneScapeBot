package scripts.api;

import org.tribot.script.sdk.WorldHopper;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.World;

public class MyWorldHopper {
    public static boolean hopToRandomMemberWorldWithRequirements(){
        return Query
                .worlds()
                .isMembers()
                .isNotDangerous()
                .isRequirementsMet()
                .isNotAnyType(World.Type.PVP_ARENA)
                .isNotAnyType(World.Type.BOUNTY)
                .isNotAnyType(World.Type.TOURNAMENT)
                .isNotAnyType(World.Type.HIGH_RISK)
                .isNotAnyType(World.Type.LAST_MAN_STANDING)
                .isNotAnyType(World.Type.LEAGUE)
                .isNotAnyType(World.Type.PVP)
                .isNotAnyType(World.Type.DEADMAN)
                .isNotCurrentWorld()
                .findRandom()
                .map(i -> WorldHopper.hop(i.getWorldNumber()))
                .orElse(false);
    }

    public static boolean hopToRandomFreeToPlayWorldWithRequirements(){
        return Query
                .worlds()
                .isNonMembers()
                .isNotDangerous()
                .isRequirementsMet()
                .isNotAnyType(World.Type.PVP_ARENA)
                .isNotAnyType(World.Type.BOUNTY)
                .isNotAnyType(World.Type.TOURNAMENT)
                .isNotAnyType(World.Type.HIGH_RISK)
                .isNotAnyType(World.Type.LAST_MAN_STANDING)
                .isNotAnyType(World.Type.LEAGUE)
                .isNotAnyType(World.Type.PVP)
                .isNotAnyType(World.Type.DEADMAN)
                .isNotCurrentWorld()
                .findRandom()
                .map(i -> WorldHopper.hop(i.getWorldNumber()))
                .orElse(false);
    }
}
