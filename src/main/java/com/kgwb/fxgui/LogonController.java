package com.kgwb.fxgui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class LogonController implements Initializable {
    private static final String STR_STAGE_TITLE = "Mini-Link MAC";
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private ImageView loginIcon;
    @FXML private TextField userTextField;
    @FXML private PasswordField pwBox;
    @FXML private Text actionResultText;
    @FXML private Button logonButton;
    @FXML private Button cancelButton;

    public LogonController() {
    }

    public void initialize(URL location, ResourceBundle resources) {
        Image loginIconImage = new Image(getClass().getResourceAsStream("Security_Permission.png"));
        this.loginIcon.setImage(loginIconImage);
        this.actionResultText.setVisible(false);
        this.logonButton.setDisable(true);
        this.userTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            this.logonButton.setDisable(newValue.trim().isEmpty());
            this.actionResultText.setVisible(false);
        });
        this.logonButton.setDefaultButton(true);
        this.cancelButton.setCancelButton(true);

        Platform.runLater(() -> {
            this.userTextField.requestFocus();

            //Remove to keep login form active
            try {
                Parent parent = null;
                parent = FXMLLoader.load(this.getClass().getResource("mainc.fxml"));
                Stage applicationStage = (Stage) logonButton.getScene().getWindow();
                applicationStage.hide();
                applicationStage.setTitle(STR_STAGE_TITLE);
                applicationStage.setMinHeight(600.0D);
                applicationStage.setMinWidth(800.0D);
                Scene mainScene = new Scene(parent, 1024.0D, 768.0D);
//                mainScene.getStylesheets().add(this.getClass().getResource("JMetroBase.css").toExternalForm());
//                mainScene.getStylesheets().add(this.getClass().getResource("JMetroDarkTheme.css").toExternalForm());
//                mainScene.getStylesheets().add(this.getClass().getResource("mainc.css").toExternalForm());
//                mainScene.getStylesheets().add(this.getClass().getResource("JMetroLightTheme.css").toExternalForm());
                applicationStage.setScene(mainScene);
                applicationStage.show();
                applicationStage.setMaximized(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void logonButtonAction(ActionEvent actionEvent) throws IOException {
        if(!userTextField.getText().contentEquals("demo")) {
            actionResultText.setText("Invalid username or password.");
            this.actionResultText.setVisible(true);
            return;
        }

        Parent parent = FXMLLoader.load(this.getClass().getResource("mainc.fxml"));
        Stage applicationStage = (Stage)((Node)actionEvent.getSource()).getScene().getWindow();
        applicationStage.hide();
        applicationStage.setTitle(STR_STAGE_TITLE);
        applicationStage.setMinHeight(600.0D);
        applicationStage.setMinWidth(800.0D);
        Scene mainScene = new Scene(parent, 1024.0D, 768.0D);
        applicationStage.setScene(mainScene);
        applicationStage.show();
        applicationStage.setMaximized(true);
    }

    public void cancelButtonAction() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Confirmation");
        alert.setHeaderText("Exit Application?");
        alert.setContentText("Are you sure to exit from application?");
        alert.initStyle(StageStyle.UTILITY);
        alert.initModality(Modality.APPLICATION_MODAL);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            Platform.exit();
        }

    }
}
