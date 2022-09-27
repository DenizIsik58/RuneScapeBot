package scripts.rev;

import org.tribot.script.sdk.*;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.tasks.Amount;
import org.tribot.script.sdk.tasks.BankTask;
import org.tribot.script.sdk.tasks.EquipmentReq;
import org.tribot.script.sdk.types.Widget;
import scripts.api.MyBanker;
import scripts.api.MyClient;

import java.util.ArrayList;
import java.util.List;
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

    private BankTask fletchingTask = null;


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

            if (!MyBanker.openBank()) {
                MyBanker.openBank();
            }
            while (!canContinue()) {

                performFletching();
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

    private void restockFletching(){
        List<String> itemsToBuy = new ArrayList<>();

        if (Bank.getCount("Feather") < 1000) {
            Log.debug("We are out of feather");
            itemsToBuy.add("Feather");
        }

        if (Bank.getCount("Arrow shaft") < 1000) {
            Log.debug("We are out of arrow shaft");
            itemsToBuy.add("Arrow shaft");
        }
        if (itemsToBuy.size() == 0) {
            return;
        }
        GrandExchangeRevManager.restockFromBank(itemsToBuy);
    }

    public void performFletching(){
        // If we don't have our setup ready. Get it ready
        if (!Inventory.contains(53, 314)) {
            if (!MyBanker.openBank()) {
                MyBanker.openBank();
            }

            if (!MyBanker.depositInventory()) {
                MyBanker.depositInventory();
            }
            Waiting.waitNormal(1500, 100);

            restockFletching();

            MyBanker.withdraw(52, 7000, false);
            MyBanker.withdraw(314, 7000, false);

            if (!MyBanker.closeBank()){
                MyBanker.closeBank();
            }
        }
        // Else start fletching
        if (!MyPlayer.isAnimating()){
            Query.inventory()
                    .idEquals(314)
                    .findFirst()
                    .map(feather -> Query.inventory()
                            .idEquals(52)
                            .findFirst()
                            .map(shaft -> shaft.useOn(feather))
                            .orElse(false));

            var seen = Waiting.waitUntil(() -> MyClient.isWidgetVisible(270, 14));
            if (seen) {
                Query.widgets()
                        .actionContains("Make sets")
                        .inIndexPath(270, 14)
                        .findFirst()
                        .map(Widget::click);
                var animating = Waiting.waitUntil(MyPlayer::isAnimating);

                if (animating) {
                    Waiting.waitNormal(50000, 15000);
                }
            }
        }
    }

    private BankTask getFletchingTask(){
        if (fletchingTask == null) {
            fletchingTask = BankTask.builder()
                    .addInvItem(52, Amount.of(Bank.getCount(52)))
                    .addInvItem(314, Amount.of(Bank.getCount(314)))
                    .build();
        }
        return fletchingTask;
    }

    private boolean isFletchingBankTaskSatisfied(){
        return getFletchingTask().isSatisfied();
    }
}
