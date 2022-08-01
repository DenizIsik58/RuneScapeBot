package scripts.api;

import java.util.Random;

public class MyAntiBan {

    private static final Random random = new Random();


    public static boolean roll() {
        return roll(50);
    }

    // int 0 to 100
    public static boolean roll(int chance) {
        return randomBoolean(chance);
    }

    public static boolean randomBoolean(int chance) {
        return random.nextInt(100) < chance;
    }

}
