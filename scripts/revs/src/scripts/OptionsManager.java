package scripts;

import org.tribot.script.sdk.Camera;
import org.tribot.script.sdk.GameState;
import org.tribot.script.sdk.Options;
import org.tribot.script.sdk.util.ScriptSettings;

public class OptionsManager {

    public static void setSoundOff(){
        if (Options.isAnySoundOn()) {
            Options.turnAllSoundsOff();
        }
    }

    public static void setRunOn(){
        if (!Options.isRunEnabled()) {
            Options.setRunEnabled(true);
        }
    }

    public static void setAidOff(){
        if (Options.isAcceptAidEnabled()) {
            Options.setAcceptAid(false);
        }
    }

    public static void setZoomScrollable(){
        if (!Options.isZoomWithScrollEnabled()) {
            Options.setZoomWithScrollEnabled(true);
        }
    }

    public static void setPkerProtectionOn(){
        if (GameState.getVarbit(13131) != 1) {

        }
    }




    public static void init(){
        setAidOff();
        setRunOn();
        setSoundOff();
        setZoomScrollable();
        setPkerProtectionOn();
    }
}
