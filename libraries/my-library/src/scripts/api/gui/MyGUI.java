package scripts.api.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.tribot.script.sdk.Log;
import org.tribot.script.sdk.Waiting;

import javax.swing.*;
import java.net.URL;

/**
 * @author Laniax
 */
public class MyGUI extends Application {


    private final URL fxml;
    private final URL stylesheet;

    private Stage stage;
    private Scene scene;
    private MyAbstractGui controller;
    private boolean decorated = true;

    private boolean isOpen = false;

    public MyGUI(URL fxml) {
        this(fxml, null);
    }

    public MyGUI(URL fxml, boolean decorated) {
        this(fxml, null, decorated);
    }

    public MyGUI(URL fxml, URL stylesheet) {
        this(fxml, stylesheet, true);
    }

    public MyGUI(URL fxml, URL stylesheet, boolean decorated) {

        this.fxml = fxml;
        this.stylesheet = stylesheet;
        this.decorated = decorated;

        // We have to start the JFX thread from the EDT otherwise tribot will end it.
        SwingUtilities.invokeLater(() -> {

            new JFXPanel(); // we have to init the toolkit

            Platform.runLater(() -> {
                try {
                    final Stage stage = new Stage();

                    if (!this.decorated) {
                        stage.initStyle(StageStyle.TRANSPARENT);
                    }

                    start(stage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        waitForInit();
    }


    public Scene getScene() {
        return this.scene;
    }

    public Stage getStage() {
        return this.stage;
    }

    /**
     * The main entry point for all JavaFX applications.
     * The start method is called after the init method has returned,
     * and after the system is ready for the application to begin running.
     * <p>
     * <p>
     * NOTE: This method is called on the JavaFX Application Thread.
     * </p>
     *
     * @param stage the primary stage for this application, onto which
     *              the application scene can be set. The primary stage will be embedded in
     *              the browser if the application was launched as an applet.
     *              Applications may create other stages, if needed, but they will not be
     *              primary stages and will not be embedded in the browser.
     */
    @Override
    public void start(Stage stage) throws Exception {

        if (fxml == null) {
            Log.error("fxml is null. aborting");
            return;
        }

        this.stage = stage;


//        stage.setAlwaysOnTop(true);

        Platform.setImplicitExit(false);

        FXMLLoader loader = new FXMLLoader(fxml);

        // By default FXMLLoader uses a different classloader, this caused issues with upcasting
        loader.setClassLoader(this.getClass().getClassLoader());

        Parent box = loader.load();

        controller = loader.getController();

        if (controller == null) {
            Log.error("Please add a controller to your fxml!");
            return;
        }

        controller.setGUI(this);

        scene = new Scene(box);

        if (!this.decorated) {
            scene.setFill(Color.TRANSPARENT);
        }

        if (this.stylesheet != null)
            scene.getStylesheets().add(this.stylesheet.toExternalForm());

        stage.setScene(scene);

    }

    public <T extends MyAbstractGui> T getController() {
        return (T) this.controller;

    }

    public void show() {

        if (stage == null)
            return;

        isOpen = true;

        Platform.runLater(() -> stage.show());
    }

    public void close() {

        if (stage == null)
            return;

        isOpen = false;

        Platform.runLater(() -> stage.close());
    }

    public boolean isOpen() {
        return isOpen;
    }

    private void waitForInit() {

        Waiting.waitUntil(5000, () -> {

            Waiting.wait(250);
            return stage != null;
        });
    }
}