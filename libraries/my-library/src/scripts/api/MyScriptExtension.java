package scripts.api;

import org.jetbrains.annotations.NotNull;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.ScriptListening;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.script.ScriptConfig;
import org.tribot.script.sdk.script.TribotScript;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class MyScriptExtension implements TribotScript {

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean hasRunEnding = new AtomicBoolean(false);
    private ScriptSetup setup;


    protected abstract void setupScript(ScriptSetup setup);
    protected abstract void onStart(String args);
    protected abstract void onMainLoop();
    protected abstract void onEnding();

    @Override
    public void configure(@NotNull ScriptConfig config) {
        // we create our own
        setup = new ScriptSetup();
        // this lets the sub class or whatever its called alter it
        setupScript(setup);
        // then we can use it
        config.setBreakHandlerEnabled(setup.breakHandlerEnabled);
        config.setRandomsAndLoginHandlerEnabled(setup.loginHandlerEnabled);

    }

    @Override
    public void execute(@NotNull String args) {

        ScriptListening.addPreEndingListener(() -> {
            running.set(false);
            if (!hasRunEnding.get()) {
                hasRunEnding.set(true);
                onEnding();
            }
        });

        MyScriptVariables.init(this);
        MyCamera.init();
        MyOptions.init();

        if (setup.mainLoopWaitsForLogin) MyClient.waitUntilLoggedIn();

        onStart(args);

        while (running.get()) {
            onMainLoop();
            Waiting.wait(setup.mainLoopInterval);
        }


        Log.info("Script complete!");
    }

    protected static class ScriptSetup {
        private boolean breakHandlerEnabled = true;
        private boolean loginHandlerEnabled = true;
        private boolean mainLoopWaitsForLogin = true;
        private int mainLoopInterval = 50;

        public ScriptSetup disableWaitForLogin() {
            this.mainLoopWaitsForLogin = false;
            return this;
        }
        public ScriptSetup setMainLoopInterval(int interval) {
            mainLoopInterval = interval;
            return this;
        }
        public ScriptSetup disableBreakHandler() {
            breakHandlerEnabled = false;
            return this;
        }
        public ScriptSetup disableLoginHandler() {
            loginHandlerEnabled = false;
            return this;
        }
    }

}
