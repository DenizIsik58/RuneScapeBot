package scripts;

import org.tribot.script.sdk.antiban.Antiban;
import org.tribot.script.sdk.antiban.AntibanProperties;

public class AntiBanManager {

    public static void init(){
        AntibanProperties.getPropsForCurrentChar().setRunEnergyMean(100);
        AntibanProperties.getPropsForCurrentChar().setUseFKeysForTabsChance(80);
        Antiban.shouldCloseWithEscape();
        Antiban.setScriptAiAntibanEnabled(true);
        AntibanProperties.getPropsForCurrentChar().setPreHoverBankChance(40);
        AntibanProperties.getPropsForCurrentChar().setHpPercentToEatStd(60);
        AntibanProperties.getPropsForCurrentChar().setUseMinimapWhileScreenWalkingChance(45);
        AntibanProperties.getPropsForCurrentChar().setUseScreenWalkingChance(20);

    }
}
