package scripts.api;

import org.tribot.script.sdk.query.Query;

public class Client {

    public static boolean clickWidget(String action, int... indexPath) {
        return Query.widgets()
                .inIndexPath(indexPath)
                .actionContains(action)
                .findFirst()
                .map(widget -> widget.click(action))
                .orElse(false);
    }

    public static boolean isWidgetVisible(int... indexPath) {
        return Query.widgets()
                .inIndexPath(indexPath)
                .isVisible()
                .isAny();
    }

}
