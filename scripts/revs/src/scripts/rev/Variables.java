package scripts.rev;




import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Variables {

    private static volatile Variables instance = null;
    private String status = "Starting Script";
    private String scriptName = "";
    private final AtomicBoolean quitting = new AtomicBoolean(false);

    private final Map<String, Object> variables = new HashMap<>();

    private Variables() {
    }

    public static Variables get() {
        Variables result = instance;
        if (result == null) {
            synchronized (Variables.class) {
                result = instance;
                if (result == null) {
                    instance = result = new Variables();
                }
            }
        }
        return result;
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