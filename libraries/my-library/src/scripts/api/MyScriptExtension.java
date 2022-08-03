package scripts.api;

import org.jetbrains.annotations.NotNull;
import org.tribot.script.sdk.*;
import org.tribot.script.sdk.painting.Painting;
import org.tribot.script.sdk.painting.template.basic.BasicPaintTemplate;
import org.tribot.script.sdk.painting.template.basic.PaintLocation;
import org.tribot.script.sdk.painting.template.basic.PaintRows;
import org.tribot.script.sdk.painting.template.basic.PaintTextRow;
import org.tribot.script.sdk.script.ScriptConfig;
import org.tribot.script.sdk.script.TribotScript;


import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class MyScriptExtension implements TribotScript {

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean hasRunEnding = new AtomicBoolean(false);
    private ScriptSetup setup;
    private BasicPaintTemplate leftPaint;
    private BasicPaintTemplate rightPaint;

    private final Color textColor = Color.cyan.darker();
    private final Color backgroundColor = Color.darkGray.darker();


    protected abstract void setupScript(ScriptSetup setup);
    protected void setupPaint(BasicPaintTemplate.BasicPaintTemplateBuilder paint) {}
    protected abstract void onStart(String args);
    protected abstract void onMainLoop();
    protected abstract void onEnding();

    protected PaintTextRow.PaintTextRowBuilder getTextRowTemplate() {
        return PaintTextRow.builder()
                .textColor(textColor)
                .background(backgroundColor)
                .borderColor(textColor)
                .borderStroke(new BasicStroke());
    }

    @Override
    public void configure(@NotNull ScriptConfig config) {
        // we create our own
        setup = new ScriptSetup();
        // this lets the sub class or whatever its called alter it
        setupScript(setup);
        // then we can use it
        config.setBreakHandlerEnabled(setup.breakHandlerEnabled);
        config.setRandomsAndLoginHandlerEnabled(setup.loginHandlerEnabled);

        leftPaint = BasicPaintTemplate.builder()
                .location(PaintLocation.BOTTOM_LEFT_VIEWPORT)
                .row(PaintRows.scriptName(getTextRowTemplate()))
                .row(PaintRows.runtime(getTextRowTemplate()))
                .row(getTextRowTemplate().label("Profit").value(MyScriptVariables::getProfit).build())
                .row(getTextRowTemplate().label("Ranged level").value(String.valueOf(Skill.RANGED.getCurrentLevel())).build())
                .row(getTextRowTemplate().label("Deaths").value(MyScriptVariables::getDeathString).build())
                .row(getTextRowTemplate().label("Membership days left").value(String.valueOf(MyPlayer.getMembershipDaysRemaining())).build())
                .row(getTextRowTemplate().label("Times muled").value(MyScriptVariables::getTimesMuled).build())
                .row(getTextRowTemplate().label("Status").value(MyScriptVariables::getStatus).build())
                .build();

        var rightBuilder = BasicPaintTemplate.builder()
                .location(PaintLocation.BOTTOM_RIGHT_VIEWPORT);
        setupPaint(rightBuilder);

        rightPaint = rightBuilder.build();

        Painting.addPaint(leftPaint::render);
        Painting.addPaint(rightPaint::render);

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
