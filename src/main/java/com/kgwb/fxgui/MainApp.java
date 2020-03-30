package com.kgwb.fxgui;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MainApp extends Application {
    private static final String STR_STAGE_TITLE = "Log On | Mini-Link MAC";

    public MainApp() { }

    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(this.getClass().getResource("logonForm.fxml"));
        primaryStage.getIcons().add(new Image(MainApp.class.getResourceAsStream("settings.png")));
        primaryStage.setTitle(STR_STAGE_TITLE);
        primaryStage.setMinHeight(240.0D);
        primaryStage.setMinWidth(400.0D);
        Scene logonScene = new Scene(root, 380.0D, 200.0D);
        primaryStage.setScene(logonScene);
        primaryStage.setOnCloseRequest(new MainApp.ExitButtonListener());
     //   primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private class ExitButtonListener implements EventHandler<WindowEvent> {
        private ExitButtonListener() {
        }

        public void handle(WindowEvent event) {
           /* Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Cancel Confirmation");
            alert.setHeaderText("Exit Application?");
            alert.setContentText("Are you sure to exit from application?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                Platform.exit();
            } else {
                event.consume();
            }*/

        }
    }
}
