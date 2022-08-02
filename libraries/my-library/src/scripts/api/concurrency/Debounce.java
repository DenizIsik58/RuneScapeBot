package scripts.api.concurrency;

import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.util.Retry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/* Written by IvanEOD 6/26/2022, at 4:07 PM */
public class Debounce {

    private final AtomicBoolean debounced = new AtomicBoolean(false);
    private final Debouncer debouncer = () -> debounced.set(false);
    private final int interval;
    private final TimeUnit timeUnit;
    private final String name;

    private static int standardWaitUntilTimeout = 10000;
    private static int standardWaitUntilAttempts = 6;


    public Debounce(String name, int interval, TimeUnit unit) {
        this.name = name + " Debounce";
        this.interval = interval;
        this.timeUnit = unit;
    }

    public Debounce(int interval, TimeUnit unit) {
        this.name = "Debounce";
        this.interval = interval;
        this.timeUnit = unit;
    }

    public boolean isDebounced() { return debounced.get(); }
    public boolean isNotDebounced() { return !debounced.get(); }

    public void setDebounced(boolean value) {
        debounced.set(value);
    }

    public void debounce() {
        debounced.set(true);
        DebounceManager.call(debouncer, interval, timeUnit);
    }

    public boolean waitUntilNotDebounced(int timeout, int attempts) {
        return Retry.retry(attempts, () -> Waiting.waitUntil(timeout, () -> !debounced.get()));
    }

    public boolean waitUntilNotDebounced() {
        return Retry.retry(standardWaitUntilAttempts, () -> Waiting.waitUntil(standardWaitUntilTimeout, () -> !debounced.get()));
    }

    public boolean waitUntilNotDebounced(BooleanSupplier debounceCondition, int timeout, int attempts) {
        return Retry.retry(attempts, () -> Waiting.waitUntil(timeout, () -> {
            if (debounceCondition.getAsBoolean()) debounce();
            return !debounced.get();
        }));
    }

    public boolean ifNotDebounced(Runnable runnable) {
        return ifNotDebounced(false, runnable);
    }

    public boolean ifNotDebounced(boolean failExtendsDebounce, Runnable runnable) {
        if (debounced.get()) {
            if (failExtendsDebounce) debounce();
            return false;
        }
        runnable.run();
        debounce();
        return true;
    }

    public boolean whenNotDebounced(Runnable runnable) {
        if (!debounced.get()) {
            runnable.run();
            debounce();
            return true;
        }
        if (waitUntilNotDebounced()) {
            runnable.run();
            debounce();
            return true;
        }
        return false;
    }


    // Call method after timer;
    // Additional calls extend time;
    // Additional calls do not extend time and replace previous call, ignoring the previous call;
    // Additional calls do not extend time and are ignored and only the original unblocked call is executed;

    // Call method before timer;
    // If timer is already running, additional calls are ignored;


    // Call method after X time without condition being true;





    // Additional calls replace the previous call and will be called after timer.
    // Additional calls extend the delay

    public enum DebouncePolicy {
        EXTEND,
        BLOCK,
        MONITOR,
        THROTTLE
    }


}
