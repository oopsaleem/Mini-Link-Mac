package com.kgwb.fxgui;

import com.kgwb.LongRunningTask;
import com.kgwb.model.MiniLinkMacWrapper;
import com.kgwb.model.MacModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.*;


public class Controller implements Initializable {

    private int proActiveVal = 10;
    private TableView<MacModel> tableView;
    private ScheduledExecutorService ses;
    private ScheduledFuture<?> scheduledFuture;

    @FXML
    private Spinner<Integer> spinnerProactive;
    @FXML
    private Button btnExport;
    @FXML
    private AnchorPane contentAnchorPane;
    @FXML
    private ProgressBar progress;
    @FXML
    private Label rightStatusLabel;
    @FXML
    private TextField urlTextEntry;
    @FXML
    private TextField filterTextEntry;
    @FXML
    private Button btnChangePathAndRun;
    @FXML
    private Button btnRun;

    public void initialize(URL location, ResourceBundle resources) {
        tableView = new TableView<>();
        btnExport.setDisable(true);
        spinnerProactive.getValueFactory().setValue(proActiveVal);
        ((SpinnerValueFactory.IntegerSpinnerValueFactory)spinnerProactive.getValueFactory()).setAmountToStepBy(1);

        contentAnchorPane.getChildren().add(tableView);
        AnchorPane.setBottomAnchor(tableView, 0.0);
        AnchorPane.setTopAnchor(tableView, 0.0);
        AnchorPane.setLeftAnchor(tableView, 0.0);
        AnchorPane.setRightAnchor(tableView, 0.0);

        urlTextEntry.setPromptText("Click [Select File ...] *.xlsx");

        btnChangePathAndRun.setOnAction(event -> {
            if (progress.visibleProperty().get()) return;
            if (FileSourceNotReady(event, true)) return;
            startProcess();
        });

        btnRun.setOnAction(event -> {
            if (FileSourceNotReady(event, false)) return;
            startProcess();
        });

        btnExport.setOnAction(event -> {
            Stage primaryStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("MS Excel files (*.xls)", "*.xlsx");
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(extFilter);
            File file = chooser.showSaveDialog(primaryStage);

            if (file == null) return;

            SXSSFWorkbook workbook = new SXSSFWorkbook(-1); // keep 100 rows in memory, exceeding rows will be flushed to disk
            Sheet spreadsheet = workbook.createSheet();

            Row row = spreadsheet.createRow(0);

            int colSize = tableView.getColumns().size();
            int itemSize = tableView.getItems().size();

            for (int j = 0; j < colSize; j++) {
                row.createCell(j).setCellValue(tableView.getColumns().get(j).getText());
            }

            for (int r = 0; r < itemSize; r++) {
                row = spreadsheet.createRow(r + 1);
                for (int c = 0; c < colSize; c++) {

                    Object fxCellData = tableView.getColumns().get(c).getCellData(r);
                    if (fxCellData != null) {
                        row.createCell(c).setCellValue(fxCellData.toString());
                    } else {
                        row.createCell(c).setCellValue("");
                    }
                }
            }

            FileOutputStream fileOut;
            try {
                fileOut = new FileOutputStream(file);
                workbook.write(fileOut);
                fileOut.close();
                java.awt.Desktop.getDesktop().open(file);
            } catch (FileNotFoundException e) {
//                messageBox("Export Error", e.getMessage());
                exceptionDialog("Export Error", "Could not export file !", file.getPath(), e);
            } catch (IOException e) {
                exceptionDialog("Export Error", "Could not export file due to Input/Output Error.", file.getPath(), e);
            } finally {
                workbook.dispose();
            }
        });
    }

    private boolean FileSourceNotReady(ActionEvent event, boolean forceChange) {
        String filePath = urlTextEntry.getText();
        Stage primaryStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        String browsedPath;
        File file = new File(filePath);

        if (file.exists() && !forceChange)
            browsedPath = file.getAbsolutePath();
        else {
            FileChooser fileChooser = new FileChooser();
            if (file.exists()) {
                fileChooser.setInitialDirectory(file.getParentFile());
            }
            File fileSelected = fileChooser.showOpenDialog(primaryStage);
            browsedPath = fileSelected != null ? fileSelected.getAbsolutePath() : null;
        }

        if (browsedPath == null) return true;
        urlTextEntry.setText(browsedPath);
        return false;
    }

    //    private AtomicBoolean isProcessStarted = new AtomicBoolean(false);
    private void startProcess() {
        //tableView.getItems().clear();
        btnRun.setText("Stop");
        progress.setVisible(true);

        File file = new File(urlTextEntry.getText());
        if (!(file.exists() || !file.isDirectory())) return;

        final LongRunningTask task = new LongRunningTask(urlTextEntry.getText());

        rightStatusLabel.textProperty().bind(task.messageProperty());
        progress.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded((succeededEvent) -> {
            btnRun.setText("Start");
            progress.setVisible(false);
            btnExport.setDisable(false);
//            spinnerProactive.setDisable(false);
//            isProcessStarted.set(false);
            populateTable(task.getValue());
        });

        task.setOnCancelled(cancelledEvent -> {
            btnRun.setText("Start");
            progress.setVisible(false);
//            spinnerProactive.setDisable(false);
            btnExport.setDisable(true);
//            isProcessStarted.set(false);
        });

        task.setOnFailed((failedEvent) -> {
            btnRun.setText("Start");
            progress.setVisible(true);
            btnExport.setDisable(true);
//            spinnerProactive.setDisable(false);
//            isProcessStarted.set(false);
            rightStatusLabel.textProperty().unbind();
            rightStatusLabel.setText(String.format("Failed when %s", rightStatusLabel.getText()));
            exceptionDialog("Processing Failure", "Processing failed to finish.",
                    "We experienced ", new Exception(task.getException()));
        });

        btnRun.setOnAction(event -> {
            if (task.isRunning()) {
                task.cancel();
                return;
            }
            if (FileSourceNotReady(event, false)) return;
            startProcess();
        });

//        spinnerProactive.setDisable(true);
//        isProcessStarted.set(true);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(task);
        executorService.shutdown();
    }

    public int convertNetmaskToCIDR(InetAddress netmask) {

        byte[] netmaskBytes = netmask.getAddress();
        int cidr = 0;
        boolean zero = false;
        for (byte b : netmaskBytes) {
            int mask = 0x80;

            for (int i = 0; i < 8; i++) {
                int result = b & mask;
                if (result == 0) {
                    zero = true;
                } else if (zero) {
                    throw new IllegalArgumentException("Invalid netmask.");
                } else {
                    cidr++;
                }
                mask >>>= 1;
            }
        }
        return cidr;
    }

    //https://code.makery.ch/blog/javafx-dialogs-official/
    private void exceptionDialog(String title, String headerText, String contentText, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        // Create expandable Exception.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        // Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);
        alert.initStyle(StageStyle.UTILITY);
        alert.showAndWait();
    }

    private void populateTable(List<MiniLinkMacWrapper> objectList) {
//        tableView.getItems().clear();
        tableView.getColumns().clear();
        tableView.setPlaceholder(new Label("Loading..."));

        proActiveVal = spinnerProactive.getValueFactory().getValue();
        Map<String, String> fieldCaption = new LinkedHashMap<>();

        fieldCaption.put("Device Name", "nearName");
        fieldCaption.put("Device IP", "nearIpAddress");
        fieldCaption.put("Remarks", "summary");

        fieldCaption.forEach((k, v) -> {
            TableColumn<MacModel, String> column = new TableColumn<>(k);
            column.setId(v + "Col");
            column.setCellValueFactory(new PropertyValueFactory<>(v));
            tableView.getColumns().add(column);
        });

        if (objectList != null && objectList.size() > 0) {

            ObservableList<MacModel> modelData = FXCollections.observableArrayList();
            objectList.forEach(mlWrpr -> modelData.add(new MacModel((mlWrpr))));

            //Filter capability
            FilteredList<MacModel> filteredData = new FilteredList<>(modelData, p -> true);
            filterTextEntry.textProperty().addListener((observable, oldValue, newValue) ->
                    filteredData.setPredicate(model -> {
                        if (newValue == null || newValue.isEmpty()) {
                            return true;
                        }

                        String lowerCaseFilter = newValue.toLowerCase();

                        if (model.getNearName().toLowerCase().contains(lowerCaseFilter)) {
                            return true;
                        } else return model.getNearIpAddress().contains(lowerCaseFilter);
                    }));

            SortedList<MacModel> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(tableView.comparatorProperty());
            tableView.setItems(sortedData);

        }
    }

    private String getExcelColumnName(int columnNumber) {
        int dividend = columnNumber;
        String columnName = "";
        int modulo;

        while (dividend > 0) {
            modulo = (dividend - 1) % 26;
            columnName = String.format("%s%s", (char) (65 + modulo), columnName);
            dividend = (dividend - modulo) / 26;
        }

        return columnName;
    }

}
