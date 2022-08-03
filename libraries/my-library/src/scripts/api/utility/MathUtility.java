package scripts.api.utility;

import org.tribot.script.sdk.pricing.Pricing;
import scripts.api.MyScriptVariables;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class MathUtility {

    public static int osrsMax = 2147483647;
    public static long oneSecond = 1000;
    public static long oneMinute = oneSecond * 60;
    public static long oneHour = oneMinute * 60;
    private static LocalDateTime startTime = null;

    private static LocalDateTime getStartTime() {
        if (startTime == null) startTime = MyScriptVariables.getStartTime();
        return startTime;
    }

    public static boolean isEven(double number) {
        return isEven((int) Math.floor(number));
    }

    public static boolean isEven(int number) {
        return number % 2 == 0;
    }

    public static double getMinutes(long time) {
        return (((double) time / 1000) / (double) (oneMinute / 1000));
    }

    public static double getSeconds(long time) {
        return getMinutes(time) * 60;
    }

    public static String getMinutesSecondsString(long time) {
        int secondsTotal = (int) getSeconds(time);
        int minutes = Math.floorDiv(secondsTotal, 60);
        int seconds = secondsTotal - (minutes * 60);
        return (minutes + " minutes " + seconds + " seconds");
    }

    public static float getTimeSinceInMinutes(LocalDateTime startTime) {
        long timeRan = DatesUtility.durationFromNow(startTime, ChronoUnit.MILLIS);
        return (float) (((double) timeRan / 1000) / (double) (oneMinute / 1000));
    }


    public static float getTimeRanInMinutes() { return getTimeSinceInMinutes(getStartTime()); }

    public static float getTimeRanInHoursSince(LocalDateTime startTime) {
        return getTimeSinceInMinutes(startTime) / 60.0f;
    }

    public static float getRatePerHourSince(LocalDateTime startTime, float amount) {
        return round(amount / getTimeRanInHoursSince(startTime), 0.5f);
    }

    public static float getRatePerHour(float amount) {
        return getRatePerHourSince(getStartTime(), amount);
    }

    public static String getProfitPerHourSinceString(LocalDateTime startTime, Integer amount) {
        int profitPerHour = Math.floorDiv((int) round(getRatePerHourSince(startTime, amount), 0.5f), 1000);
        int ratePerHour = (int) round(getRatePerHourSince(startTime, amount), 0.5f);
        return (amount) / 1000 + "K gp [" + profitPerHour + "K gp/hr]";
    }

    public static String getProfitPerHourString(Integer amount) {
        return getProfitPerHourSinceString(getStartTime(), amount);
    }


    public static String getProfitPerHourSinceString(LocalDateTime startTime, Integer itemId, Integer amount) {
        Integer price = Pricing.lookupPrice(itemId).orElse(0);
        int profitPerHour = Math.floorDiv((int) round(getRatePerHourSince(startTime, price * amount), 0.5f), 1000);
        int ratePerHour = (int) round(getRatePerHourSince(startTime, amount), 0.5f);
        return amount + " [" + ratePerHour + "/hr] " + (price * amount) / 1000 + "K gp [" + profitPerHour + "K gp/hr]";
    }

    public static String getProfitPerHourString(int id, int amount) {
        return getProfitPerHourSinceString(getStartTime(), id, amount);
    }

    public static String getRatePerHourSinceString(LocalDateTime startTime, Integer itemId, Integer amount) {
        return amount + " [" + getRatePerHourSince(startTime, amount) + "/hr]";
    }

    public static String getRatePerHourString(int id, int amount) {
        return getRatePerHourSinceString(getStartTime(), id, amount);
    }

    public static int getPercent(int valueOne, float percent) {
        return (int) ((float) valueOne * percent);
    }

    public static int getPercent(int valueOne, int valueOf) {
        return (int) (((float) valueOne / (float) valueOf) * 100f);
    }

    public static String getRangeLevelRate(int startLevel, int newLevel){
        var difference = newLevel - startLevel;
        return "[" + startLevel + "( " + difference + " gained)]";
    }

    public static float round(float input, float step) {
        return (Math.round(input / step) * step);
    }



}
