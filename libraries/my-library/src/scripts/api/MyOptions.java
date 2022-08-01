package scripts.api;

import org.tribot.script.sdk.GameState;
import org.tribot.script.sdk.GameTab;
import org.tribot.script.sdk.MyPlayer;
import org.tribot.script.sdk.input.Mouse;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.InventoryItem;

import static scripts.api.MyClient.clickWidget;
import static scripts.api.MyClient.isWidgetVisible;

public class MyOptions {

    public static void setSoundOff(){
        if (org.tribot.script.sdk.Options.isAnySoundOn()) {
            org.tribot.script.sdk.Options.turnAllSoundsOff();
        }
    }

    public static void setOnPkSkullPrevention(){

        if (GameState.getVarbit(13131) != 1){
            if (!org.tribot.script.sdk.Options.Tab.CONTROLS.open()){
                org.tribot.script.sdk.Options.Tab.CONTROLS.open();
            }
            if (isWidgetVisible(116, 5)){
                clickWidget("Toggle skull prevention", 116, 5);
            }
        }

    }

    public static void setRunOn(){
        if (!org.tribot.script.sdk.Options.isRunEnabled()) {
            org.tribot.script.sdk.Options.setRunEnabled(true);
        }

        if (MyPlayer.getRunEnergy() < 20){
            Query.inventory().nameContains("Stamina potion").findFirst().map(InventoryItem::click);
        }
    }

    public static void setAidOff(){
        if (org.tribot.script.sdk.Options.isAcceptAidEnabled()) {
            org.tribot.script.sdk.Options.setAcceptAid(false);
        }
    }

    public static void setZoomScrollable(){
        if (!org.tribot.script.sdk.Options.isZoomWithScrollEnabled()) {
            org.tribot.script.sdk.Options.setZoomWithScrollEnabled(true);
        }
    }

    public static void setPkerProtectionOn(){
        if (GameState.getVarbit(13131) != 1) {

        }
    }
    public static boolean closeAllSettings(){
        if (org.tribot.script.sdk.Options.isAllSettingsOpen()){
            return org.tribot.script.sdk.Options.closeAllSettings();
        }
        return false;
    }



    public static void init(){
        GameTab.setSwitchPreference(GameTab.SwitchPreference.KEYS);
        Mouse.setSpeed(200);
        setAidOff();
        setRunOn();
        setSoundOff();
        setZoomScrollable();
        setPkerProtectionOn();
        setOnPkSkullPrevention();
        closeAllSettings();
    }

}