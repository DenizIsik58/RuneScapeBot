package scripts.api;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyScriptVariables {


    private static volatile MyScriptVariables instance = null;
    private final MyScriptExtension script;
    private String status = "Starting Script";
    private String scriptName = "";
    private final AtomicBoolean quitting = new AtomicBoolean(false);

    private final Map<String, Object> variables = new HashMap<>();

    private MyScriptVariables(MyScriptExtension script) {
        this.script = script;
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
        if (!vars.variables.containsKey(key)) vars.variables.put(key, defaultValue);
        return (VariableType) vars.variables.get(key);
    }

    @SuppressWarnings("unchecked")
    public static <VariableType> VariableType getVariable(String key) {
        return (VariableType) get().variables.get(key);
    }

    public static void setVariable(String key, Object value) {
        get().variables.put(key, value);
    }


    //VARIABLE METHODS
    private static final String MOUSE_SPEED = "mouseSpeed";

    public static int mouseSpeed() {
        return getVariable(MOUSE_SPEED, 200);
    }

    public static void setMouseSpeed(int value) {
        getVariable(MOUSE_SPEED, value);
    }



}
