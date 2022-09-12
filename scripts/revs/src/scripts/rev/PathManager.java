package scripts.rev;

import org.tribot.api2007.types.RSTile;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.interfaces.Positionable;
import org.tribot.script.sdk.types.World;
import org.tribot.script.sdk.types.WorldTile;
import org.tribot.script.sdk.walking.LocalWalking;
import org.tribot.script.sdk.walking.WalkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PathManager {



    public static RSTile getRSTile(WorldTile tile) {
        return new RSTile(tile.getX(), tile.getY(), tile.getPlane());
    }

    public static List<WorldTile> getPathToPool(){
        return new ArrayList<>(Arrays.asList( new WorldTile(3150, 3634, 0),
                new WorldTile(3148, 3629, 0),
                new WorldTile(3140, 3629, 0),
                      new WorldTile(3134, 3634, 0),
                                new WorldTile(3128, 3635, 0))
        );
    }

    public static List<WorldTile> getRandomPathToFlee(){
        return new ArrayList<> (Arrays.asList(new WorldTile(3102, 3656, 0),
                new WorldTile(3102, 3649, 0),
                new WorldTile(3102, 3640, 0),
                new WorldTile(3106, 3631, 0),
                new WorldTile(3106, 3623, 0),
                new WorldTile(3106, 3618, 0),
                new WorldTile(3106, 3610, 0),
                new WorldTile(3104, 3604, 0),
                new WorldTile(3103, 3598, 0),
                new WorldTile(3103, 3590, 0),
                new WorldTile(3103, 3581, 0),
                new WorldTile(3105, 3576, 0),
                new WorldTile(3104, 3570, 0),
                new WorldTile(3101, 3564, 0),
                new WorldTile(3098, 3555, 0),
                new WorldTile(3096, 3545, 0),
                new WorldTile(3096, 3533, 0),
                new WorldTile(3096, 3528, 0),
                new WorldTile(3096, 3522, 0))) ;

        /*var second = new WorldTile[] {
                new WorldTile(3102, 3656, 0),
                new WorldTile(3102, 3650, 0),
                new WorldTile(3102, 3644, 0),
                new WorldTile(3102, 3638, 0),
                new WorldTile(3102, 3629, 0),
                new WorldTile(3102, 3621, 0),
                new WorldTile(3102, 3613, 0),
                new WorldTile(3102, 3606, 0),
                new WorldTile(3102, 3598, 0),
                new WorldTile(3102, 3593, 0),
                new WorldTile(3102, 3586, 0),
                new WorldTile(3104, 3578, 0),
                new WorldTile(3106, 3573, 0),
                new WorldTile(3104, 3563, 0),
                new WorldTile(3102, 3554, 0),
                new WorldTile(3100, 3544, 0),
                new WorldTile(3099, 3533, 0),
                new WorldTile(3099, 3523, 0)
        };

        return MyAntiBan.roll() ? first : second;*/
    }

    public static List<WorldTile> getPathToCaveEntrance(){
        return new ArrayList<WorldTile> (Arrays.asList(
                new WorldTile(3125, 3629, 0),
                new WorldTile(3118, 3629, 0),
                new WorldTile(3110, 3629, 0),
                new WorldTile(3103, 3631, 0),
                new WorldTile(3097, 3634, 0),
                new WorldTile(3092, 3637, 0),
                new WorldTile(3082, 3647, 0),
                new WorldTile(3075, 3652, 0)));


    }

    public static void walkPath(List<WorldTile> path){

        var map = MyRevsClient.getScript().getMap();
        var furthestIndex = -1;

        for (var tile : path) {
            if (map.canReach(tile)) {
                var index = path.indexOf(tile);
                if (index > furthestIndex) {
                    furthestIndex = index;
                }
            }
            Log.info("Furthest index: " + furthestIndex);
            var furthestTile = path.get(furthestIndex);
            Log.info("Furthest tile: " + furthestTile);
            var pathToWalk = map.getPath(furthestTile);

            LocalWalking.walkPath(pathToWalk, () -> {
                if (LootingManager.hasPkerBeenDetected()) {
                    MyRevsClient.getScript().setState(State.BANKING);
                    return WalkState.FAILURE;
                }

                return WalkState.CONTINUE;
            });
        }

    }
}
