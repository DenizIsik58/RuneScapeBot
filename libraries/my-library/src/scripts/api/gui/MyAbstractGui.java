package scripts.api.gui;

import javafx.fxml.Initializable;

/**
 * @author Laniax
 */

public abstract class MyAbstractGui implements Initializable {


    private MyGUI gui = null;

    public void setGUI(MyGUI gui) {
        this.gui = gui;
    }

    public MyGUI getGUI() {
        return this.gui;
    }
}