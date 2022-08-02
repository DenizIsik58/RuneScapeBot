package scripts.api;

import kotlin.ranges.IntRange;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.Prayer;
import org.tribot.script.sdk.Skill;
import org.tribot.script.sdk.util.TribotRandom;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MyPrayer {

    private final AtomicReference<IntRange> drinkPrayerPotionRange = new AtomicReference<>(new IntRange(15, 25));
    private final AtomicInteger nextDrinkPrayerPotionPercent = new AtomicInteger(0);
    private static MyPrayer instance = null;

    private MyPrayer() {}

    public static MyPrayer getInstance() {
        if (instance == null) instance = new MyPrayer();
        return instance;
    }

    public static int getCurrentPrayerPercent() {
        double myPrayerLevel = Skill.PRAYER.getActualLevel();
        double myPrayer = Prayer.getPrayerPoints();
        return (int)((myPrayer / myPrayerLevel) * 100);
    }

    public static boolean shouldDrinkPrayerPotion() {
        int prayerPercent = getCurrentPrayerPercent();
        Log.trace("Current prayer percent = " + prayerPercent);
        return getNextPrayerDrinkPercent() >= prayerPercent;
    }

    public static int getNextPrayerDrinkPercent() {
        var next = getInstance().nextDrinkPrayerPotionPercent.get();
        if (next == 0) return calculateNextPrayerDrinkPercent();
        return next;
    }

    public static int calculateNextPrayerDrinkPercent() {
        IntRange range = getInstance().drinkPrayerPotionRange.get();
        var min = range.getStart();
        var max = range.getEndInclusive();
        var next = TribotRandom.uniform(min, max);
        Log.trace("Generated next prayer potion drink percent: " + next);
        getInstance().nextDrinkPrayerPotionPercent.set(next);
        return next;
    }

    public static void setPrayerDrinkPercentRange(int min, int max) {
        getInstance().drinkPrayerPotionRange.set(new IntRange(min, max));
    }

}
