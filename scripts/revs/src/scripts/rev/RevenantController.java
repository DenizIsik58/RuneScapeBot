package scripts.rev;

import com.sun.javafx.menu.MenuItemBase;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import org.tribot.script.sdk.Log;
import scripts.api.gui.MyAbstractGui;

import java.net.URL;
import java.util.ResourceBundle;

public class RevenantController extends MyAbstractGui {

    @FXML
    ToggleButton skulledButton;
    @FXML
    private Button startButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        startButton.setOnAction(event -> {
            Log.debug("Starting script!");

            if (skulledButton.isSelected()){
                MyRevsClient.getScript().setSkulledScript(true);
            }
            this.getGUI().close();
        });
    }



}
