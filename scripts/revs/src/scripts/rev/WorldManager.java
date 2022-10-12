package scripts.rev;

import org.tribot.api2007.GameTab;
import org.tribot.script.sdk.WorldHopper;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.World;
import scripts.api.MyClient;

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
                .worldNumberNotEquals(403, 404, 407, 408, 411, 412, 502, 503, 540, 541, 549, 550, 568, 569, 576, 581)
                .isNotCurrentWorld()
                .findRandom()
                .map(i ->  {
                    /*if (!GameTab.TABS.LOGOUT.isOpen()) {
                        GameTab.TABS.LOGOUT.open();
                    }

                    if (MyClient.isWidgetVisible(182, 3)) {
                        MyClient.clickWidget("World Switcher", 182, 3);
                    }

                    return Query.widgets()
                            .textContains(i.toString())
                            .actionEquals("Switch")
                            .findFirst()
                            .map(widget -> widget.click("Switch")).orElse(false);*/
                    return WorldHopper.hop(i.getWorldNumber());
                } )
                .orElse(false);
    }
}
