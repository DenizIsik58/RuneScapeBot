package scripts.rev;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import scripts.api.MyPrayer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PrayerManager {



    public static boolean isFullPrayer() {
        return Prayer.getPrayerPoints() >= Skill.PRAYER.getCurrentLevel();
    }

    public static boolean canUsePrayer(Prayer prayer){
        return Skill.PRAYER.getActualLevel() >= prayer.getRequiredLevel() && prayer.isUnlocked();

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
        return Prayer.disableAll(Prayer.values());
    }

    public static boolean enablePrayer(Prayer prayer){
        if (!prayer.isEnabled() && canUsePrayer(prayer)){
            if (Prayer.getPrayerPoints() == 0) return false;
            return Waiting.waitUntil(100, prayer::enable);
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
                    var restoreDoses = getInventoryDoseCount("restore");
                    return potion.click("Drink")
                            && Waiting.waitUntil(1000, () -> getInventoryDoseCount("restore") < restoreDoses || isFullPrayer());
                }).orElse(false);
        if (drankPotion) {
            MyPrayer.calculateNextPrayerDrinkPercent();
        }
    }

    private final static List<Integer> brewIds = List.of(-1, 1111, 1112, 1113, 1114);

    private static int getDoseCountByName(String name) {
        var cleanedName = name.replaceAll("\\D", "");
        return cleanedName.isEmpty() ? 0 : Integer.parseInt(cleanedName);
    }

    public static int getInventoryDoseCount(String name) {
        return Query.inventory().nameContains(name).stream().mapToInt(item -> getDoseCountByName(item.getName())).sum();
    }

    public static int getBrewDoseCount(){
        AtomicInteger count = new AtomicInteger();
        Inventory.getAll().forEach(item -> {
            if (item.getName().equals("Saradomin brew(1)")){
                count.addAndGet(1);
            }else if (item.getName().equals("Saradomin brew(2)")){
                count.addAndGet(2);
            }else if (item.getName().equals("Saradomin brew(3)")){
                count.addAndGet(3);
            }else if (item.getName().equals("Saradomin brew(4)")){
                count.addAndGet(4);
            }
        });
        return count.get();
    }

    public static void init(){
        setQuickPrayer();
    }
}
