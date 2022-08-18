package scripts;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import org.tribot.script.sdk.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TimerTask;

public class WCController extends AbstractGui{


    public Button loadButton;
    public ToggleButton bankLogs;
    public ChoiceBox<String> locationMenu;
    public Button startButton;
    public TextArea hopAt;
    private boolean bankLogsEnabled = false;
    private final List<String> locationChoices = List.of("Grand Exchange", "Edgeville");
    private String locationSelection = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        bankLogs.setOnAction(event -> {
            bankLogsEnabled = bankLogs.isSelected();
            Log.info("Bank logs enabled: " + bankLogsEnabled);
        });


        hopAt.setOnKeyReleased(event -> {
            WoodcuttingScript.setHopAtX(Integer.parseInt(hopAt.getText()));
            Log.debug(hopAt.getText());
        });


        loadButton.setOnAction(event -> {
            loadButton.setText("Loaded!");

            new java.util.Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    loadButton.setText("Load!");
                }
            }, 2000);

            Equipment.getAll().forEach(equipmentItem -> {
                WCEquipmentManager.getEquipment().put(equipmentItem.getSlot(), equipmentItem.getId());
            });
            WCEquipmentManager.getEquipment().forEach((k, v) -> Log.debug("KEY: " + k + " VALUE: " + v));

        });

        startButton.setOnAction(event -> {
            Log.debug("Starting script!");

            if (bankLogs.isSelected()) {
                WoodcuttingScript.setBankLogs(true);
            }
            this.getGUI().close();
        });

        locationMenu.getSelectionModel().selectedItemProperty().addListener(this::onSelectionChanged);
        locationMenu.getItems().addAll(locationChoices);
        locationMenu.setValue("Grand Exchange");
    }

    private void onSelectionChanged(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        Log.trace("Selection changed: " + newValue);
        locationSelection = newValue;
        WoodcuttingScript.setIsGeChosenForYew(true);
    }
}
