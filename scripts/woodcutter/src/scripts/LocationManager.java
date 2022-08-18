package scripts;

import lombok.Getter;
import org.tribot.script.sdk.types.WorldTile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocationManager {
    @Getter
    private static final WorldTile lumbridge = new WorldTile(3230,3233,0);
    @Getter
    private static final WorldTile draynor = new WorldTile(3085,3235,0);
    @Getter
    private static final WorldTile GE = new WorldTile(3161,3486,0);

    private static final WorldTile edgeville = new WorldTile(3087, 3475, 0);

    @Getter
    private static final WorldTile varrock = new WorldTile(3165, 3417, 0);
    @Getter
    private static final List<String> logs = new ArrayList<>(Arrays.asList("logs", "Oak logs","Willow logs", "Yew logs"));


    public static WorldTile getLumbridge() {
        return lumbridge;
    }

    public static WorldTile getDraynor() {
        return draynor;
    }

    public static WorldTile getGE() {
        return GE;
    }

    public static WorldTile getVarrock() {
        return varrock;
    }

    public static WorldTile getEdgeville() {
        return edgeville;
    }

    public static List<String> getLogs() {
        return logs;
    }
}
