package scripts.rev;

import org.tribot.script.sdk.Prayer;
import org.tribot.script.sdk.Skill;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.query.Query;
import scripts.api.MyPrayer;

public class PrayerManager {



    public static boolean isFullPrayer() {
        return Prayer.getPrayerPoints() >= Skill.PRAYER.getCurrentLevel();
    }

    public static void enableQuickPrayer(){
        if (!Prayer.isQuickPrayerEnabled()) {
            Waiting.waitUntil(Prayer::enableQuickPrayer);
        }
    }

    public static void disableQuickPrayer(){
        if (Prayer.isQuickPrayerEnabled()) {
            Prayer.disableQuickPrayer();
        }
    }

    public static boolean turnOffAllPrayer() {
        Prayer.disableAll(Prayer.values());
        return false;
    }

    public static boolean enablePrayer(Prayer prayer){
        if (!prayer.isEnabled()){
            return Waiting.waitUntil(prayer::enable);
        }
        return false;
    }

    public static void setQuickPrayer(){
        if (!Prayer.isQuickPrayersSelected(Prayer.PROTECT_ITEMS, Prayer.EAGLE_EYE)){
            Prayer.selectQuickPrayers(Prayer.PROTECT_ITEMS, Prayer.EAGLE_EYE);
        }
    }

    public static void maintainPrayerPotion(){
        var drankPotion = Query.inventory().nameContains("Blighted super restore")
                .findClosestToMouse()
                .map(potion -> {
                    var currentPrayer = MyPrayer.getCurrentPrayerPercent();
                    return potion.click("Drink")
                            && Waiting.waitUntil(1000, () -> MyPrayer.getCurrentPrayerPercent() > currentPrayer || isFullPrayer());
                }).orElse(false);
        if (drankPotion) {
            MyPrayer.calculateNextPrayerDrinkPercent();
        }
    }

    public static void init(){
        setQuickPrayer();
    }
}
