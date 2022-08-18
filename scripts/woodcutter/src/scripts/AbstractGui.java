package scripts;

import javafx.fxml.Initializable;

/**
 * @author Laniax
 */

public abstract class AbstractGui implements Initializable {


    private GUI gui = null;

    public void setGUI(GUI gui) {
        this.gui = gui;
    }

    public GUI getGUI() {
        return this.gui;
    }
}