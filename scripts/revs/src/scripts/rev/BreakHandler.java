package scripts.rev;

import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.Login;
import org.tribot.script.sdk.Waiting;
import scripts.api.MyBanker;

import java.util.Random;

public class BreakHandler {

    private long timerStart;
    private int minutesToRunBeforeBreakMin;
    private int minutesToRunBeforeBreakMax;
    private int minutesToBreakMin;
    private int minutesToBreakMax;
    private int randomMinutesToBreak = -1;
    private int randomMinutes = -1;
    private long breakStart;
    private Random random = new Random();


    public BreakHandler(int minutesToRunBeforeBreakMin, int minutesToRunBeforeBreakMax, int minutesToBreakMin, int minutesToBreakMax) {
        this.minutesToRunBeforeBreakMin = minutesToRunBeforeBreakMin;
        this.minutesToRunBeforeBreakMax = minutesToRunBeforeBreakMax;
        this.minutesToBreakMin = minutesToBreakMin;
        this.minutesToBreakMax = minutesToBreakMax;
        timerStart = System.currentTimeMillis();
    }


    public void startBreak(){
        if (canBreak()) {
            Log.debug("Starting break! Afking in bank");

            while (!canContinue()) {
                if (!MyBanker.openBank()) {
                    MyBanker.openBank();
                }
                // just wait until we can continue again
                Waiting.wait(1000);
            }
        }
    }

    public int getTimeToBreak() {
        return (randomMinutes +  minutesToRunBeforeBreakMin) * (1000 * 60);
    }

    public boolean canBreak() {
        if (randomMinutes == -1) {
            randomMinutes = random.nextInt(minutesToRunBeforeBreakMax - minutesToRunBeforeBreakMin) + minutesToRunBeforeBreakMin;
        }

        if (System.currentTimeMillis() - timerStart > randomMinutes * (1000 * 60)){
            Log.debug("Breaking after running for: " + (randomMinutes) + " minutes");
            // we need to break
            // Set timerstart to 0 and start break timer since we are in break
            timerStart = 0;
            breakStart = System.currentTimeMillis();
            randomMinutes = random.nextInt(minutesToRunBeforeBreakMax - minutesToRunBeforeBreakMin) + minutesToRunBeforeBreakMin;
            Log.debug("Next break in: " + randomMinutes);
            return true;
        }
        return false;
    }

    public boolean canContinue(){
        if (randomMinutesToBreak == -1) {
            randomMinutesToBreak = (random.nextInt(minutesToBreakMax - minutesToBreakMin) + minutesToBreakMin);
        }

        Log.debug("Breaking for : " + randomMinutesToBreak + " minutes");

        if (System.currentTimeMillis() - breakStart > randomMinutesToBreak * (1000 * 60)){
            Log.debug("Break is over after: " + (randomMinutesToBreak) + " minutes");
            // Reset timers tart until next break again
            timerStart = System.currentTimeMillis();
            // Set break timer to 0
            breakStart = 0;
            randomMinutesToBreak = (random.nextInt(minutesToBreakMax - minutesToBreakMin) + minutesToBreakMin);
            Log.debug("Next break lasts: " + randomMinutesToBreak);
            return true;
        }
        return false;
    }
}
