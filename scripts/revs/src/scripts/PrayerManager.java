package scripts;

import org.tribot.script.sdk.Prayer;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.InventoryItem;

public class PrayerManager {

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

    public static void setQuickPrayer(){
        if (!Prayer.isQuickPrayersSelected(Prayer.PROTECT_ITEMS, Prayer.EAGLE_EYE)){
            Prayer.selectQuickPrayers(Prayer.PROTECT_ITEMS, Prayer.EAGLE_EYE);
        }
    }

    public static void sipPrayer(){
            Query.inventory().nameContains("Prayer potion").findClosestToMouse().map(InventoryItem::click);
    }

    public static void init(){
        setQuickPrayer();
    }
}
