package scripts.api;

import org.tribot.script.sdk.*;
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

    public static void setRoofsOff(){
        if (Options.isRoofsEnabled()) {
            Options.setRemoveRoofsEnabled(true);
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

    public static void setCombatSettings(){
        if (Combat.isAutoRetaliateOn()){
            Combat.setAutoRetaliate(false);
        }

        if (Options.AttackOption.getNpcAttackOption() != Options.AttackOption.LEFT_CLICK_WHERE_AVAILABLE){
            Options.AttackOption.setNpcAttackOption(Options.AttackOption.LEFT_CLICK_WHERE_AVAILABLE);
        }
    }

    public static void setRunOn(){
        if (!org.tribot.script.sdk.Options.isRunEnabled()) {
            org.tribot.script.sdk.Options.setRunEnabled(true);
        }

        if (MyPlayer.getRunEnergy() < 50){
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
        Options.AttackOption.setPlayerAttackOption(Options.AttackOption.LEFT_CLICK_WHERE_AVAILABLE);
        Options.AttackOption.setNpcAttackOption(Options.AttackOption.LEFT_CLICK_WHERE_AVAILABLE);
        Mouse.setSpeed(300);
        setAidOff();
        setRunOn();
        setSoundOff();
        setZoomScrollable();
        setPkerProtectionOn();
        setCombatSettings();
        setRoofsOff();
        setOnPkSkullPrevention();
        closeAllSettings();
    }

}
