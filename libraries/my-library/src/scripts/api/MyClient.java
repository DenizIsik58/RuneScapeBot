package scripts.api;

import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.Login;
import org.tribot.script.sdk.Waiting;
import org.tribot.script.sdk.query.Query;
import org.tribot.script.sdk.types.Widget;
import org.tribot.script.sdk.util.Retry;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class MyClient {

    public static JFrame findTRiBotFrame() {
        Frame[] frames = JFrame.getFrames();
        if (frames[0] != null) return (JFrame) frames[0];
        Log.error("Error, could not find TRiBot Frame.");
        return null;
    }

    public static JRootPane getRootPane() {
        return (JRootPane) JFrame.getFrames()[0].getComponent(0);
    }


    public static boolean clickWidget(String action, int... indexPath) {
        return Query.widgets()
                .inIndexPath(indexPath)
                .actionContains(action)
                .findFirst()
                .map(widget ->  widget.click(action))
                .orElse(false);
    }

    public static boolean isWidgetVisible(int... indexPath) {
        return Query.widgets()
                .inIndexPath(indexPath)
                .isVisible()
                .isAny();
    }

    //<editor-fold desc="waitUntilLoggedIn support methods">
    private static Optional<Widget> getClickToPlayButton() {
        return Query.widgets().inIndexPath(378, 78).findFirst();
    }
    private static boolean isClickToPlayVisible() {
        return getClickToPlayButton().map(Widget::isVisible).orElse(false);
    }
    private static void clickClickToPlay() {
        getClickToPlayButton().ifPresent(button -> button.click("Play"));
    }
    //</editor-fold>

    public static boolean waitUntilLoggedIn() {
        boolean success = Retry.retry(5, () -> {
            if (isClickToPlayVisible()) clickClickToPlay();
            return Waiting.waitUntil(Login::isLoggedIn);
        });
        if (!success) {
            Login.login();
            waitUntilLoggedIn();
        }
        return Login.isLoggedIn();
    }



}
