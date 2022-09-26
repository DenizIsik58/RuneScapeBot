package scripts.rev;

import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.Login;
import org.tribot.script.sdk.Waiting;

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
    private int randomness;
    private Random random = new Random();


    public BreakHandler(int minutesToRunBeforeBreakMin, int minutesToRunBeforeBreakMax, int minutesToBreakMin, int minutesToBreakMax, int randomness) {
        this.minutesToRunBeforeBreakMin = minutesToRunBeforeBreakMin;
        this.minutesToRunBeforeBreakMax = minutesToRunBeforeBreakMax;
        this.minutesToBreakMin = minutesToBreakMin;
        this.minutesToBreakMax = minutesToBreakMax;
        this.randomness = randomness;
        timerStart = System.currentTimeMillis();
    }


    public void startBreak(){
        if (canBreak()) {
            Log.debug("Startin break!");
            // logout
            while (Login.isLoggedIn()) {
                    Login.logout();
                    Waiting.waitNormal(1000, 100);
            }

            while (!canContinue()) {
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
            randomMinutes = (random.nextInt(randomness * 2) - randomness);
        }
        if (System.currentTimeMillis() - timerStart > (randomMinutes +  minutesToRunBeforeBreakMin) * (1000 * 60)){
            Log.debug("Breaking after: " + randomMinutes + minutesToRunBeforeBreakMin + " minutes");
            // we need to break
            // Set timerstart to 0 and start break timer since we are in break
            timerStart = 0;
            breakStart = System.currentTimeMillis();
            randomMinutes = -1;
            return true;
        }
        return false;
    }

    public boolean canContinue(){
        if (randomMinutesToBreak == -1) {
            randomMinutesToBreak = (random.nextInt(minutesToBreakMax - minutesToBreakMin) + minutesToBreakMin);
        }
        Log.debug("random number: " + randomMinutesToBreak);
        if (System.currentTimeMillis() - breakStart > randomMinutesToBreak * (1000 * 60)){
            Log.debug("Break is over after: " + (randomMinutesToBreak * (1000 * 60)) + " minutes");
            // Reset timers tart until next break again
            timerStart = System.currentTimeMillis();
            // Set break timer to 0
            breakStart = 0;
            randomMinutesToBreak = -1;
            return true;
        }
        return false;
    }
}
