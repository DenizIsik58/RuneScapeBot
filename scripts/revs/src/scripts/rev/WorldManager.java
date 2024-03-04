package scripts.rev;

import org.tribot.api2007.GameTab;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.WorldHopper;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.World;
import scripts.api.MyClient;
import scripts.kt.api.utility.WorldHopping;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WorldManager {

    private static List<Integer> worlds = new ArrayList<>(
            Arrays.asList(302, 303, 304, 305, 306, 307, 309, 310, 311, 312,
                    313, 314, 315, 317, 320, 321, 322, 323, 324, 325, 327,
                    328, 329, 330, 331, 332, 333, 334, 336, 337, 338, 339,
                    340, 341, 342, 343, 344, 346, 347, 348, 350, 351, 352,
                    353, 354, 355, 356, 357, 358, 359, 360, 362, 367, 368,
                    369, 370, 374, 375, 376, 377, 378, 386, 387, 388, 389,
                    390, 395, 421, 422, 424, 443,
                    444, 445, 446, 463, 464, 465, 466, 477, 478, 479, 480,
                    481, 482, 484, 485, 486, 487, 488, 489, 490, 491, 492,
                    493, 494, 495, 496, 505, 506, 507, 508, 509, 510, 511,
                    512, 513, 514, 515, 516, 517, 518, 519, 520, 521, 522,
                    523, 524, 525, 531, 532, 535, 559));

    public static boolean hopToRandomMemberWorldWithRequirements(){
        //return WorldHopping.hopWorlds();
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
                        World.Type.SKILL_TOTAL,
                        World.Type.QUEST_SPEEDRUNNING)
                .worldNumberNotEquals(403, 404, 407, 408, 411, 412, 502, 503, 527, 427, 540, 541, 549, 550, 568, 569, 576, 581)
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
