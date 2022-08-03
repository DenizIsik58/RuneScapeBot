package scripts.api;

import org.tribot.script.sdk.Log;

import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MyScriptVariables {


    private static volatile MyScriptVariables instance = null;
    private final MyScriptExtension script;
    private String scriptName = "";
    private final AtomicBoolean quitting = new AtomicBoolean(false);
    private final AtomicReference<String> status = new AtomicReference<>("Starting script");
    private final AtomicReference<String> profitString = new AtomicReference<>("0 gp");
    private final AtomicReference<String> deathString = new AtomicReference<>("0");
    private final AtomicReference<String> timesMuledString = new AtomicReference<String>("0");
    private final LocalDateTime startTime;


    private final Map<String, Object> variables = new HashMap<>();

    private MyScriptVariables(MyScriptExtension script) {
        this.script = script;
        startTime = LocalDateTime.now();
    }

    public static LocalDateTime getStartTime() {
        return get().startTime;
    }

    public static MyScriptVariables init(MyScriptExtension script) {
        MyScriptVariables result = instance;
        if (result == null) {
            synchronized (MyScriptVariables.class) {
                result = instance;
                if (result == null) {
                    instance = result = new MyScriptVariables(script);
                }
            }
        }
        return result;
    }

    public static MyScriptVariables get() {
        if (instance == null) throw new RuntimeException("Script Variables weren't initialized before calling get()");
        return instance;
    }

    @SuppressWarnings("unchecked")
    public static <ScriptClass extends MyScriptExtension> ScriptClass getScript() {
        return (ScriptClass) get().script;

    }

    @SuppressWarnings("unchecked")
    public static <VariableType> VariableType getVariable(String key, VariableType defaultValue) {
        var vars = get();
        try {
            if (!vars.variables.containsKey(key)) vars.variables.put(key, defaultValue);
            return (VariableType) vars.variables.get(key);
        } catch(ConcurrentModificationException ignore) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    public static <VariableType> VariableType getVariable(String key) {
        try {
            return (VariableType) get().variables.get(key);
        } catch (ConcurrentModificationException ignore) {
            return null;
        }
    }

    private static final AtomicInteger setAttempts = new AtomicInteger(0);
    public static void setVariable(String key, Object value) {
        try {
            get().variables.put(key, value);
        } catch (ConcurrentModificationException ignore) {
            Log.error("Variable was not set because concurrent modification exception?");
            if (setAttempts.incrementAndGet() < 3) {
                setVariable(key, value);
                return;
            }
        }
        setAttempts.set(0);
    }


    //VARIABLE METHODS
    private static final String MOUSE_SPEED = "mouseSpeed";

    public static int mouseSpeed() {
        return getVariable(MOUSE_SPEED, 200);
    }

    public static void setMouseSpeed(int value) {
        getVariable(MOUSE_SPEED, value);
    }

    public static void updateStatus(String status) {
        get().status.set(status);
    }

    public static String getStatus() {
        return get().status.get();
    }


    public static void setProfit(String profitString) {
        get().profitString.set(profitString);
    }

    public static String getProfit(){
        return get().profitString.get();
    }

    public static void setDeath(String amount){
        get().deathString.set(amount);
    }

    public static String getDeathString() {
        return get().deathString.get();
    }

    public static String getTimesMuled() {
        return get().timesMuledString.get();
    }

    public static void setTimesMuled(String newValue){
        get().timesMuledString.set(newValue);
    }
}
