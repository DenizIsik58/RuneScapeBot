package scripts.rev;

import org.tribot.script.sdk.Prayer;
import org.tribot.script.sdk.Skill;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.query.Query;
import scripts.api.MyAntiBan;

public class PrayerManager {

    public static int getCurrentPrayerPercent() {
        double myPrayerLevel = Skill.PRAYER.getActualLevel();
        double myPrayer = Prayer.getPrayerPoints();
        return (int)((myPrayer / myPrayerLevel) * 100);
    }

    public static boolean isFullPrayer() {
        return Prayer.getPrayerPoints() >= Skill.PRAYER.getCurrentLevel();
    }

    public static void enableQuickPrayer(){
        if (!Prayer.isQuickPrayerEnabled()) {
            Prayer.enableQuickPrayer();
        }
    }

    public static void disableQuickPrayer(){
        if (Prayer.isQuickPrayerEnabled()) {
            Prayer.disableQuickPrayer();
        }
    }

    public static boolean setPrayer(Prayer prayer){
        if (!prayer.isEnabled()){
            return prayer.enable();
        }
        return false;
    }

    public static void setQuickPrayer(){
        if (!Prayer.isQuickPrayersSelected(Prayer.PROTECT_ITEMS, Prayer.EAGLE_EYE)){
            Prayer.selectQuickPrayers(Prayer.PROTECT_ITEMS, Prayer.EAGLE_EYE);
        }
    }

    public static void maintainPrayerPotion(){
        var drankPotion = Query.inventory().nameContains("Prayer potion")
                .findClosestToMouse()
                .map(potion -> {
                    var currentPrayer = getCurrentPrayerPercent();
                    return potion.click("Drink")
                            && Waiting.waitUntil(1000, () -> getCurrentPrayerPercent() > currentPrayer || isFullPrayer());
                }).orElse(false);
        if (drankPotion) MyAntiBan.calculateNextPrayerDrinkPercent();
    }

    public static void init(){
        setQuickPrayer();
    }
}
