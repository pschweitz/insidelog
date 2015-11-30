/*
 * Copyright 2015 Philippe Schweitzer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dbiservices.monitoring.tail;

/**
 *
 * @author Philippe Schweitzer
 * @version 1.1
 * @since 16.11.2015
 */
import com.dbiservices.tools.ApplicationContext;
import com.dbiservices.tools.Logger;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.UUID;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;

public class ColorChooserWindowControler implements Initializable {

    private static final Logger logger = Logger.getLogger(ColorChooserWindowControler.class);

    @FXML
    private TableView colorTable;
    @FXML
    private TableColumn patternColumn;
    @FXML
    private TableColumn colorColumn;
    @FXML
    private TableColumn caseColumn;

    @FXML
    private TextField patternText;

    @FXML
    private ColorPicker patternColor;
    @FXML
    private ColorPicker defaultColor;
    @FXML
    private ColorPicker backgroundColor;
    @FXML
    private ColorPicker selectionColor;
    @FXML
    private ColorPicker searchColor;

    @FXML
    private Button addButton;
    @FXML
    private Button removeButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button upButton;
    @FXML
    private Button downButton;

    private ObservableList<PatternColorConfiguration> data;
    private Stage colorChooserWindowStage;
    private ColorConfiguration colorConfiguration;
    private InformationObject informationObject;

    private String fileColors = "etc/color.cfg";

    public ColorChooserWindowControler(InformationObject informationObject, String fileColors, Stage colorChooserWindowStage) {

        this.informationObject = informationObject;
        if (informationObject != null) {
            this.colorConfiguration = informationObject.getColorConfiguration();
        } else {
            this.colorConfiguration = (ColorConfiguration) ApplicationContext.getInstance().get("colorDefaultConfiguration");
        }
        
        if(this.colorConfiguration == null){
            this.colorConfiguration = (ColorConfiguration) ApplicationContext.getInstance().get("colorDefaultConfiguration");
            logger.error("colorDefaultConfiguration: " + colorConfiguration);
        }                
        
        this.colorChooserWindowStage = colorChooserWindowStage;
        this.fileColors = fileColors;
    }

    @FXML
    private void saveButtonAction(ActionEvent event) {

        ApplicationContext applicationContext = ApplicationContext.getInstance();

        if (fileColors.equals("etc/color.cfg") && informationObject.getFilePath().toString().equals(".")) {
            if (applicationContext.containsKey("colorDefaultConfiguration")) {
                applicationContext.remove("colorDefaultConfiguration");
            }
        }

        colorConfiguration = new ColorConfiguration(fileColors);
        colorConfiguration.setDefaultColor(this.defaultColor.getValue());
        colorConfiguration.setBackgroundColor(this.backgroundColor.getValue());
        colorConfiguration.setSelectionColor(this.selectionColor.getValue());
        colorConfiguration.setSearchColor(this.searchColor.getValue());

        ArrayList colorConfigurationList = new ArrayList();
        ObservableList<PatternColorConfiguration> items = colorTable.getItems();

        for (PatternColorConfiguration item : items) {
            colorConfigurationList.add(new PatternColorConfiguration(item.pattern, item.color, item.caseSentitive));
        }

        colorConfiguration.setColorConfigurationList(colorConfigurationList);

        String oldFileColorsName = new String(fileColors);

        if (fileColors.equals("etc/color.cfg") && informationObject.getFilePath().toString().equals(".")) {
            applicationContext.put("colorDefaultConfiguration", colorConfiguration);
        } else {
            fileColors = "etc/" + UUID.randomUUID().toString() + ".cfg";
            informationObject.setFileColors(fileColors);
        }

        saveColorToFile(fileColors);

        informationObject.setColorConfiguration(colorConfiguration);

        DbiTail.saveTreeToFile();

        try {
            if (!oldFileColorsName.equals("etc/color.cfg")) {
                Files.delete(Paths.get(oldFileColorsName));
            }
        } catch (IOException ex) {
            logger.error("Error deleting file: " + oldFileColorsName, ex);
        }

        colorChooserWindowStage.close();
    }

    private void saveColorToFile(String colorFileName) {

        BufferedWriter bw_colorView = null;

        Path filePath = Paths.get(colorFileName);

        try {
            Files.deleteIfExists(filePath);

            if (!Files.exists(Paths.get("etc"))) {
                Files.createDirectory(Paths.get("etc"));
            }
            Files.createFile(filePath);

            bw_colorView = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(colorFileName), Charset.forName("UTF-8")));

        } catch (IOException e) {
            logger.error("Error saving color file: " + filePath, e);
        }

        try {
            bw_colorView.write("\"" + colorConfiguration.getBackgroundColor().toString() + "\";\"" + colorConfiguration.getDefaultColor().toString() + "\";\"" + colorConfiguration.getSelectionColor().toString() + "\";\"" + colorConfiguration.getSearchColor().toString() + "\"");
            bw_colorView.newLine();
            bw_colorView.flush();
        } catch (IOException e) {
            logger.error("Error saving color file: " + filePath, e);
        }

        ArrayList<PatternColorConfiguration> colorConfigurationList = colorConfiguration.getColorConfigurationList();

        for (int i = 0; i < colorConfigurationList.size(); i++) {

            PatternColorConfiguration patternColorConfiguration = colorConfigurationList.get(i);
            try {

                bw_colorView.write("\"" + patternColorConfiguration.getPattern() + "\";\"" + patternColorConfiguration.getColor().toString() + "\";\"" + patternColorConfiguration.isCaseSentitive() + "\"");
                bw_colorView.newLine();
                bw_colorView.flush();
            } catch (IOException e) {
                logger.error("Error saving color file: " + filePath, e);
            }
        }

        try {
            bw_colorView.close();

            logger.debug("Color configuration succesfully saved");

        } catch (IOException e) {
            logger.error("Error saving color file: " + filePath, e);
        }
    }

    @FXML
    private void cancelButtonAction(ActionEvent event) {

        colorChooserWindowStage.close();
    }

    @FXML
    private void addButtonAction(ActionEvent event) {

        boolean insert = true;
        if (!patternText.getText().equals("")) {
            ObservableList<PatternColorConfiguration> items = colorTable.getItems();

            for (PatternColorConfiguration item : items) {
                if (item.pattern.equals(patternText.getText())) {
                    insert = false;
                    break;
                }
            }
            if (insert) {
                items.add(new PatternColorConfiguration(patternText.getText(), patternColor.getValue(), true));
            } else {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Warning Dialog");
                alert.setHeaderText("Pattern already in use");
                alert.setContentText("Pattern already in use: \"" + patternText.getText() + "\"");

                alert.showAndWait();
            }
        }
    }

    @FXML
    private void removeButtonAction(ActionEvent event) {

        ObservableList<PatternColorConfiguration> selectedItems = colorTable.getSelectionModel().getSelectedItems();
        ObservableList<PatternColorConfiguration> items = colorTable.getItems();

        for (PatternColorConfiguration item : selectedItems) {
            patternText.setText(item.getPattern());
            patternColor.setValue(item.getColor());
            items.remove(item);
        }
    }

    @FXML
    private void upButtonAction(ActionEvent event) {

        ObservableList<PatternColorConfiguration> items = colorTable.getItems();

        int index = colorTable.getSelectionModel().getSelectedIndex();
        PatternColorConfiguration item = (PatternColorConfiguration) colorTable.getSelectionModel().getSelectedItem();
        try {
            colorTable.getSelectionModel().select(index - 1);

            PatternColorConfiguration itemInvert = (PatternColorConfiguration) colorTable.getSelectionModel().getSelectedItem();

            items.set(index, itemInvert);

            items.set(index - 1, item);
            colorTable.getSelectionModel().select(index - 1);

        } catch (Exception e) {
        }
    }

    @FXML
    private void downButtonAction(ActionEvent event) {

        ObservableList<PatternColorConfiguration> items = colorTable.getItems();

        int index = colorTable.getSelectionModel().getSelectedIndex();
        PatternColorConfiguration item = (PatternColorConfiguration) colorTable.getSelectionModel().getSelectedItem();
        try {
            colorTable.getSelectionModel().select(index + 1);

            PatternColorConfiguration itemInvert = (PatternColorConfiguration) colorTable.getSelectionModel().getSelectedItem();

            items.set(index, itemInvert);

            items.set(index + 1, item);
            colorTable.getSelectionModel().select(index + 1);
        } catch (Exception e) {
        }
    }

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {

        if (colorTable == null) {
            logger.error("fx:id=\"colorTable\" was not injected: check your FXML file 'simple.fxml'.");
        }

        if (patternColumn == null) {
            logger.error("fx:id=\"patternColumn\" was not injected: check your FXML file 'simple.fxml'.");
        }

        if (colorColumn == null) {
            logger.error("fx:id=\"colorColumn\" was not injected: check your FXML file 'simple.fxml'.");
        }

        if (caseColumn == null) {
            logger.error("fx:id=\"caseColumn\" was not injected: check your FXML file 'simple.fxml'.");
        }

        patternColumn.setCellValueFactory(
                new PropertyValueFactory<PatternColorConfiguration, String>("pattern"));

        colorColumn.setCellValueFactory(
                new PropertyValueFactory<PatternColorConfiguration, Color>("color"));

        colorColumn.setCellFactory(new Callback<TableColumn<PatternColorConfiguration, Color>, TableCell<PatternColorConfiguration, Color>>() {
            @Override
            public TableCell<PatternColorConfiguration, Color> call(TableColumn<PatternColorConfiguration, Color> ColorConfigurationTableColumn) {
                return new ColorTableCell(ColorConfigurationTableColumn);
            }
        });

        caseColumn.setCellValueFactory(
                new PropertyValueFactory<PatternColorConfiguration, Boolean>("caseSentitive"));

        caseColumn.setCellFactory(new Callback<TableColumn<PatternColorConfiguration, Boolean>, TableCell<PatternColorConfiguration, Boolean>>() {

            @Override
            public TableCell<PatternColorConfiguration, Boolean> call(TableColumn<PatternColorConfiguration, Boolean> caseSensitiveConfigurationTableColumn) {
                return new CheckboxCell(caseSensitiveConfigurationTableColumn);
            }
        });

        data = FXCollections.observableArrayList(colorConfiguration.colorConfigurationList);
        colorTable.setItems(data);

        patternColor.setValue(colorConfiguration.defaultColor);
        defaultColor.setValue(colorConfiguration.defaultColor);
        backgroundColor.setValue(colorConfiguration.backgroundColor);
        selectionColor.setValue(colorConfiguration.selectionColor);
        searchColor.setValue(colorConfiguration.searchColor);

        upButton.setGraphic(new ImageView(new Image("arrow-up.png")));
        downButton.setGraphic(new ImageView(new Image("arrow-down.png")));
    }

    public class CheckboxCell<T> extends TableCell<PatternColorConfiguration, Boolean> {

        CheckBox checkbox;

        public CheckboxCell(TableColumn<T, Boolean> column) {

            if (checkbox == null) {
                checkbox = new CheckBox();
            }
            checkbox.selectedProperty().addListener(new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) {
                    setItem(new_val);
                    if (getTableRow() != null) {
                        ((PatternColorConfiguration) getTableView().getItems().get(getTableRow().getIndex())).caseSentitive = new_val;
                    }
                }
            });

            checkbox.setSelected(getValue());
            setText(null);
            this.setAlignment(Pos.CENTER);
            setGraphic(checkbox);
        }

        @Override
        protected void updateItem(Boolean item, boolean empty) {
            super.updateItem(item, empty);

            setText(null);
            if (empty) {
                setGraphic(null);
            } else {
                this.checkbox.setSelected(item);
                this.setGraphic(this.checkbox);
            }
        }

        private void paintCell() {
            if (checkbox == null) {
                checkbox = new CheckBox();

                checkbox.selectedProperty().addListener(new ChangeListener<Boolean>() {

                    @Override
                    public void changed(ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) {
                        setItem(new_val);
                        ((PatternColorConfiguration) getTableView().getItems().get(getTableRow().getIndex())).caseSentitive = new_val;
                    }
                });
            }
            checkbox.setSelected(getValue());
            setText(null);
            this.setAlignment(Pos.CENTER);
            setGraphic(checkbox);
        }

        private Boolean getValue() {
            return getItem() == null ? false : getItem();
        }
    }

    private class ColorTableCell<T> extends TableCell<T, Color> {

        private final ColorPicker colorPicker;

        public ColorTableCell(TableColumn<T, Color> column) {
            this.colorPicker = new ColorPicker();
            colorPicker.setMinWidth(113);
            this.colorPicker.editableProperty().bind(column.editableProperty());
            this.colorPicker.disableProperty().bind(column.editableProperty().not());
            this.colorPicker.setOnShowing(event -> {
                final TableView<T> tableView = getTableView();
                tableView.getSelectionModel().select(getTableRow().getIndex());
                tableView.edit(tableView.getSelectionModel().getSelectedIndex(), column);
            });
            this.colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (isEditing()) {
                    commitEdit(newValue);
                    ((PatternColorConfiguration) getTableView().getItems().get(getTableRow().getIndex())).color = newValue;
                }
            });
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(Color item, boolean empty) {
            super.updateItem(item, empty);

            setText(null);
            if (empty) {
                setGraphic(null);
            } else {
                this.colorPicker.setValue(item);
                this.setGraphic(this.colorPicker);
            }
        }
    }

}
