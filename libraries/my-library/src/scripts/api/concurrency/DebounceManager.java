package scripts.api.concurrency;

import org.tribot.script.sdk.ScriptListening;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/* Written by IvanEOD 6/26/2022, at 3:53 PM */
public class DebounceManager {


    private final ScheduledExecutorService debounceScheduler = Executors.newScheduledThreadPool(1);
    private final ConcurrentHashMap<Debouncer, DebounceContext> delayedMap = new ConcurrentHashMap<>();

    private DebounceManager() {
        ScriptListening.addEndingListener(this::onScriptEnding);
    }

    public void terminate() {
        try {
            debounceScheduler.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) { }
    }

    public static void call(Debouncer debouncer, int interval, TimeUnit timeUnit) {
        var scriptDebouncer = getInstance();
        DebounceContext task = new DebounceContext(debouncer, interval, timeUnit);
        DebounceContext prev;
        do {
            prev = scriptDebouncer.delayedMap.putIfAbsent(debouncer, task);
            if (prev == null) scriptDebouncer.debounceScheduler.schedule(task, interval, timeUnit);
        } while (prev != null && !prev.extend());
    }

    public static DebounceManager getInstance() {
        return DebounceManagerSingleton.INSTANCE;
    }

    private static class DebounceManagerSingleton {
        private static final DebounceManager INSTANCE = new DebounceManager();
    }

    private static class DebounceContext implements Runnable {

        private final Debouncer debouncer;
        private final int interval;
        private final TimeUnit timeUnit;
        private final Object lock = new Object();
        private long dueTime;

        public DebounceContext(Debouncer debouncer, int interval, TimeUnit timeUnit) {
            this.debouncer = debouncer;
            this.interval = interval;
            this.timeUnit = timeUnit;
            extend();
        }

        public boolean extend() {
            synchronized (lock) {
                if (dueTime < 0) return false;
                dueTime = System.currentTimeMillis() + timeUnit.toMillis(interval);
                return true;
            }
        }

        @Override
        public void run() {
            synchronized (lock) {
                long remaining = dueTime - System.currentTimeMillis();
                if (remaining > 0) {
                    getInstance().debounceScheduler.schedule(this, remaining, TimeUnit.MILLISECONDS);
                } else {
                    dueTime = -1;
                    try {
                        debouncer.call();
                    } finally {
                        getInstance().delayedMap.remove(debouncer);
                    }
                }
            }
        }
    }

    public void onScriptEnding() {
        this.terminate();
        ScriptListening.removeEndingListener(this::onScriptEnding);
    }
}