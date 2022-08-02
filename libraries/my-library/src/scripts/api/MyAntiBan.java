package scripts.api;

import kotlin.ranges.IntRange;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.MyPlayer;
import org.tribot.script.sdk.util.TribotRandom;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MyAntiBan {

    private final Random random = new Random();

    // you can pick a default min max? idk i guessed. 50-65 is fine
    private final AtomicReference<IntRange> eatPercentRange = new AtomicReference<>(new IntRange(50, 65));
    private final AtomicInteger nextEatPercent = new AtomicInteger(0);



    public static boolean shouldEat() {
        var myHp = MyPlayer.getCurrentHealthPercent();
        return getNextEatPercent() >= myHp;
    }


    public static int getNextEatPercent() {
        var next = getInstance().nextEatPercent.get();
        if (next == 0) return calculateNextEatPercent();
        return next;
    }

    // call this after successful eats
    public static int calculateNextEatPercent() {
        IntRange range = getInstance().eatPercentRange.get();
        var min = range.getStart();
        var max = range.getEndInclusive();
        var next = TribotRandom.uniform(min, max);
        Log.trace("Generated next eat percent: " + next);
        getInstance().nextEatPercent.set(next);
        return next;
    }

    public static void setEatPercentRange(int min, int max) {
        getInstance().eatPercentRange.set(new IntRange(min, max));
    }

    private static MyAntiBan instance = null;

    private MyAntiBan() {}

    public static MyAntiBan getInstance() {
        if (instance == null) instance = new MyAntiBan();
        return instance;
    }




    public static boolean roll() {
        return roll(50);
    }

    // int 0 to 100
    public static boolean roll(int chance) {
        return randomBoolean(chance);
    }

    public static boolean randomBoolean(int chance) {
        return getInstance().random.nextInt(100) < chance;
    }

}
