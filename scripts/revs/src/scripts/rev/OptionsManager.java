package scripts.rev;

import org.tribot.script.sdk.GameState;
import org.tribot.script.sdk.MyPlayer;
import org.tribot.script.sdk.Options;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.InventoryItem;

import static scripts.api.Client.clickWidget;
import static scripts.api.Client.isWidgetVisible;

public class OptionsManager {

    public static void setSoundOff(){
        if (Options.isAnySoundOn()) {
            Options.turnAllSoundsOff();
        }
    }

    public static void setOnPkSkullPrevention(){

        if (GameState.getVarbit(13131) != 1){
            if (!Options.Tab.CONTROLS.open()){
                Options.Tab.CONTROLS.open();
            }
            if (isWidgetVisible(116, 5)){
                clickWidget("Toggle skull prevention", 116, 5);
            }
        }

    }

    public static void setRunOn(){
        if (!Options.isRunEnabled()) {
            Options.setRunEnabled(true);
        }

        if (MyPlayer.getRunEnergy() < 20){
            Query.inventory().nameContains("Stamina potion").findFirst().map(InventoryItem::click);
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
        setOnPkSkullPrevention();
    }
}
