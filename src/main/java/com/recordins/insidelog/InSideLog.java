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
package com.recordins.insidelog;

/**
 * @author Philippe Schweitzer
 * @version 1.1
 * @since 16.11.2015
 */

import com.recordins.tools.ApplicationContext;
import com.recordins.tools.Logger;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeViewBuilder;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

public class InSideLog extends Application {

    private static final Logger logger = Logger.getLogger(InSideLog.class);

    public int year = 2019;
    public String version = "1.5";

    private static String[] args;
    private static String treeFileName = "etc/" + ApplicationContext.getInstance().getString("tail.defaultTreeConfiguration");
    public static String colorFileName = "etc/" + ApplicationContext.getInstance().getString("tail.defaultColorConfiguration");

    private static TreeView<String> treeView;

    private InformationObject rootInformationObject;

    private Stage mainStage;

    private HBox toolBar;
    private TreeItemNode treeRoot;
    private StackPane stackPaneInformation;

    private final TextField textFieldNodeName = new TextField();
    private final TextField textFieldFileLocation = new TextField();
    private final TextField textFieldBufferSize = new TextField("100");
    private final CheckBox displayColors = new CheckBox();

    private final TextField textFieldFrequency = new TextField("100");

    private ChoiceBox choiceBoxColorTemplate = new ChoiceBox();
    private ChoiceBox choiceBoxCharset = new ChoiceBox();

    private VBox vboxInformationFile;
    private VBox vboxInformationGroup;
    private VBox vboxInformationRoot;

    private TitledPane metricOveralDefinition;
    private GridPane gridPaneName;
    private GridPane gridPaneInformation;
    private SplitPane splitPaneCenter;

    private Label labelNodeName;
    private Button edit;
    private Button save;

    private Label labelFrequency;

    private boolean newTreeItemFlag = false;
    private boolean copyTreeItemFlag = false;
    private boolean editFlag = false;

    private File lastSelected = null;
    public static boolean showMainStage = true;

    private static TreeItemNode copiedTreeItemNode = null;
    private StackPane treePane;

    private static InSideLog instance;

    public InSideLog() {
        instance = this;
    }

    @Override
    public void start(Stage mainStage) {
        ApplicationContext applicationContext = ApplicationContext.getInstance();

        Logger.setLogLevel(Logger.LogLevel.findLevel(applicationContext.getString("logger.level")));

        if (treeFileName.equals("etc/")) {
            treeFileName = "etc/default.tree";
        }
        if (colorFileName.equals("etc/")) {
            colorFileName = "etc/default.cfg";
        }

        applicationContext.put("colorFileName", colorFileName);
        applicationContext.put("InsideLog", this);

        ColorConfiguration colorDefaultConfiguration = new ColorConfiguration();
        applicationContext.put("colorDefaultConfiguration", colorDefaultConfiguration);

        initVBoxInformation();

        initMainWindow();
        initTreeNodes();
        initApropos();

        Parameters parameters = getParameters();

        Map<String, String> namedParameters = parameters.getNamed();
        List<String> rawArguments = parameters.getRaw();
        List<String> unnamedParameters = parameters.getUnnamed();

        logger.debug("\nArguments -");
        for (String arg : args) {
            logger.debug(arg);

            openFile(arg);
        }

        initMainStage(mainStage);
    }

    /* opens a file from filesystem */
    private void openSavedFile(String fileName) {

        ArrayList<String> fileList = new ArrayList();
        InSideLog.getConfigurationFileList(Paths.get("etc").toFile(), fileList);

        ChoiceDialog<String> dialog = new ChoiceDialog<>("etc/default.cfg", fileList);
        dialog.setTitle("Select applicatble color template");
        dialog.setHeaderText("Select applicatble color template");
        dialog.setContentText("Select template: ");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String colorFile = result.get();

            if (!colorFile.startsWith("etc")) {
                colorFile = "etc/" + colorFile;
            }

            InformationObject informationObject = new InformationObject(fileName, fileName, fileName, 1000, 500, true, colorFile);
            ApplicationContext.getInstance().put(fileName, new WindowTextConsole(fileName, informationObject));

            ApplicationContext.getInstance().put(informationObject.getFullName(), new WindowTextConsole(informationObject.getFullName(), informationObject));
            TreeItemNode newTreeItemNode = new TreeItemNode(informationObject, false);

            informationObject.getWindowTextConsole().clear();

            newTreeItemNode.showTextconsole();
            informationObject.getWindowTextConsole().readSavedFile();
        }
    }

    private void openFile(String fileName) {

        if (Files.exists(Paths.get(fileName))) {
            showMainStage = false;

            TreeItemNode selectedTreeItem = (TreeItemNode) treeView.getRoot();

            if (selectedTreeItem != null) {

                ArrayList<TreeItemNode> nodes = new ArrayList();

                TreeItemNode.getRecursiveLeaf(selectedTreeItem, nodes);

                int count = 0;
                for (TreeItemNode node : nodes) {

                    try {
                        if (Files.exists(Paths.get(node.getInformationObject().getFilePath()))) {
                            if (Files.isSameFile(Paths.get(node.getInformationObject().getFilePath()), Paths.get(fileName))) {
                                count++;
                                selectedTreeItem = node;

                                InformationObject informationObject = selectedTreeItem.getInformationObject();
                                if (ApplicationContext.getInstance().containsKey(informationObject.getFullName())) {

                                    selectedTreeItem.getInformationObject().getWindowTextConsole().clear();
                                    selectedTreeItem.getInformationObject().getWindowTextConsole().setIsRunning(true);
                                } else {
                                    ApplicationContext.getInstance().put(informationObject.getFullName(), new WindowTextConsole(informationObject.getFullName(), informationObject));
                                }
                                selectedTreeItem.startTailSchedule();
                                selectedTreeItem.showTextconsole();
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error accessing file: " + fileName, e);
                    }
                }
                if (count == 0) {

                    newTreeItemFlag = true;
                    lastSelected = Paths.get(fileName).toFile();
                    logger.trace("lastSelected.getAbsolutePath(): " + lastSelected.getAbsolutePath());
                    textFieldFileLocation.setText(lastSelected.getAbsolutePath());
                    textFieldNodeName.setText(lastSelected.getName());

                    if (ApplicationContext.getInstance().containsKey("tail.displayColor")) {
                        displayColors.setSelected(ApplicationContext.getInstance().getBoolean("tail.displayColor"));
                    } else {
                        displayColors.setSelected(true);
                    }

                    if (ApplicationContext.getInstance().containsKey("tail.bufferSize")) {
                        textFieldBufferSize.setText(ApplicationContext.getInstance().getString("tail.bufferSize"));
                    } else {
                        textFieldBufferSize.setText("100");
                    }

                    if (ApplicationContext.getInstance().containsKey("tail.frequencyInterval")) {
                        textFieldFrequency.setText(ApplicationContext.getInstance().getString("tail.frequencyInterval"));
                    } else {
                        textFieldBufferSize.setText("500");
                    }

                    fireSaveButton();
                }
            }
        } else {
            logger.error("Error opening file, file not found: " + fileName);
        }
    }

    private void initApropos() {
        Label message = new Label();
        TextField fileName = new TextField();

        Button ok = new Button("ok");
        Button cancel = new Button("cancel");

        VBox popUpVBox = new VBox();
        HBox popUpHBox = new HBox();

        final Stage fileSelectorStage;

        Scene fileSelectorScene = new Scene(popUpVBox);

        fileSelectorStage = new Stage();
        fileSelectorScene.setFill(Color.web("#dce6ec"));

        fileSelectorStage.setTitle("Group name");
        fileSelectorStage.setScene(fileSelectorScene);

        fileSelectorStage.setResizable(false);

        message.setText("Group name:");
        ok = new Button("ok");
        cancel = new Button("cancel");

        ok.setPrefWidth(75);
        cancel.setPrefWidth(75);
        fileName.setMaxWidth(170);

        popUpHBox.getChildren().add(cancel);
        popUpHBox.getChildren().add(ok);
        popUpHBox.alignmentProperty().setValue(Pos.CENTER);
        popUpHBox.setSpacing(15);

        popUpVBox.getChildren().add(message);
        popUpVBox.getChildren().add(fileName);
        popUpVBox.getChildren().add(popUpHBox);
        popUpVBox.setPrefSize(200, 100);
        popUpVBox.alignmentProperty().setValue(Pos.CENTER);
        popUpVBox.setSpacing(15);

        fileName.setOnKeyReleased(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent t) {

                if (t.getCode() == KeyCode.ESCAPE) {
                    fileSelectorStage.hide();
                }
            }
        });

        ok.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                fileSelectorStage.hide();

            }
        });

        cancel.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                fileSelectorStage.hide();
            }
        });
    }

    private void initVBoxInformation() {

        stackPaneInformation = new StackPane();
        Label labelFileLocation = new Label("File Location:");

        Button buttonBrowse = new Button("Browse");
        buttonBrowse.setPrefWidth(80);
        buttonBrowse.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent e) {
                        final FileChooser fileChooser = new FileChooser();

                        if (!textFieldFileLocation.getText().equals("") && Files.exists(Paths.get(textFieldFileLocation.getText()))) {

                            fileChooser.setInitialDirectory(Paths.get(textFieldFileLocation.getText()).getParent().toFile());
                        } else if (lastSelected != null) {
                            fileChooser.setInitialDirectory(lastSelected.getParentFile());
                        }

                        final File selectedFile = fileChooser.showOpenDialog(mainStage);
                        if (selectedFile != null) {
                            lastSelected = selectedFile;
                            selectedFile.getAbsolutePath();
                            textFieldFileLocation.setText(selectedFile.getAbsolutePath());
                        }
                    }
                }
        );

        textFieldFileLocation.textProperty().addListener((observable, oldValue, newValue) -> {
            if (textFieldFileLocation.getText().contains("\\")) {
                textFieldNodeName.setText(textFieldFileLocation.getText().substring(textFieldFileLocation.getText().lastIndexOf('\\') + 1, textFieldFileLocation.getText().length()));
            }
            if (textFieldFileLocation.getText().contains("/")) {
                textFieldNodeName.setText(textFieldFileLocation.getText().substring(textFieldFileLocation.getText().lastIndexOf('/') + 1, textFieldFileLocation.getText().length()));
            }
        });

        Label labelBufferSize = new Label("Buffer size (x1K):");
        labelBufferSize.setAlignment(Pos.CENTER_RIGHT);

        Label labelColor = new Label("Display colors:");
        labelBufferSize.setAlignment(Pos.CENTER_RIGHT);

        labelFrequency = new Label("Frequency (ms):");

        Label labelColorTemplate = new Label("Color Template:");
        labelBufferSize.setAlignment(Pos.CENTER_RIGHT);
        choiceBoxColorTemplate = new ChoiceBox();
        choiceBoxColorTemplate.setPrefWidth(1000);

        Label labelCharset = new Label("Charset:");
        labelBufferSize.setAlignment(Pos.CENTER_RIGHT);
        choiceBoxCharset = new ChoiceBox();
        choiceBoxCharset.setPrefWidth(1000);

        gridPaneInformation = new GridPane();
        gridPaneInformation.setPadding(new Insets(0, 10, 10, 10));

        ColumnConstraints column11 = new ColumnConstraints();
        column11.setPercentWidth(40);
        column11.setHalignment(HPos.RIGHT);
        ColumnConstraints column12 = new ColumnConstraints();
        column12.setPercentWidth(60);
        column12.setHalignment(HPos.CENTER);
        gridPaneInformation.getColumnConstraints().addAll(column11, column12);

        gridPaneInformation.setHgap(5);
        gridPaneInformation.setVgap(10);

        gridPaneInformation.add(labelFileLocation, 0, 1);
        gridPaneInformation.add(buttonBrowse, 1, 1);
        gridPaneInformation.add(textFieldFileLocation, 1, 2);
        gridPaneInformation.add(labelBufferSize, 0, 3);
        gridPaneInformation.add(textFieldBufferSize, 1, 3);
        gridPaneInformation.add(labelColor, 0, 4);
        gridPaneInformation.add(displayColors, 1, 4);
        gridPaneInformation.add(labelColorTemplate, 0, 5);
        gridPaneInformation.add(choiceBoxColorTemplate, 1, 5);
        gridPaneInformation.add(labelCharset, 0, 6);
        gridPaneInformation.add(choiceBoxCharset, 1, 6);
        gridPaneInformation.add(labelFrequency, 0, 7);
        gridPaneInformation.add(textFieldFrequency, 1, 7);

        TitledPane targetInformation = new TitledPane();
        targetInformation.setText("Target Information");
        targetInformation.setMinWidth(150);
        targetInformation.setContent(gridPaneInformation);
        Node content = targetInformation.getContent();
        content.setStyle("-fx-background-color: #dce6e6;");

        Pane spacer1 = new Pane();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        spacer1.setMinWidth(Region.USE_PREF_SIZE);

        Pane spacer2 = new Pane();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        spacer2.setMinWidth(Region.USE_PREF_SIZE);

        Pane spacer3 = new Pane();
        HBox.setHgrow(spacer3, Priority.ALWAYS);
        spacer3.setMinWidth(Region.USE_PREF_SIZE);

        vboxInformationFile = new VBox();
        vboxInformationFile.setSpacing(15);
        vboxInformationFile.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        vboxInformationFile.getChildren().add(targetInformation);

        vboxInformationGroup = new VBox();
        vboxInformationGroup.alignmentProperty().setValue(Pos.CENTER);
        vboxInformationGroup.setSpacing(15);
        vboxInformationGroup.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Image logoImage = new Image("tail_logo.png");
        ImageView imageView = new ImageView(logoImage);
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(75);

        Image recordinsImage = new Image("recordins.png");
        ImageView imageViewrecordins = new ImageView(recordinsImage);
        imageViewrecordins.setPreserveRatio(true);
        imageViewrecordins.setFitHeight(70);

        imageViewrecordins.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                this.getHostServices().showDocument("https://www.recordins.com");
            }
        });

        Hyperlink linkRecordins = new Hyperlink();
        linkRecordins.setText("Recordins");
        linkRecordins.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                getHostServices().showDocument("https://www.recordins.com");
            }
        });

        Hyperlink linkPHS = new Hyperlink();
        linkPHS.setText("Philippe Schweitzer");
        linkPHS.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                getHostServices().showDocument("https://ch.linkedin.com/pub/philippe-schweitzer/87/ab/2bb");
            }

        });

        Hyperlink linkGitHub = new Hyperlink();
        linkGitHub.setText("GitHub page");
        linkGitHub.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent t) {
                getHostServices().showDocument("https://github.com/pschweitz/insidelog");
            }
        });

        Label labelText = new Label("in'side log - The colorized log viewer v" + version);
        Label labelYear = new Label("- Apache v2 license - " + year);
        Label labelCreator = new Label("Created by");
        Label labelGitHub = new Label("in'side log is open source software, please visit");
        Label labelRecordins1 = new Label("Create your private blockchain, faster");
        Label labelRecordins2 = new Label("with easy data model and ACLs definition");

        HBox hboxText = new HBox();
        hboxText.alignmentProperty().setValue(Pos.CENTER);
        hboxText.getChildren().add(labelText);

        HBox hboxRecordins = new HBox();
        hboxRecordins.alignmentProperty().setValue(Pos.CENTER);
        hboxRecordins.getChildren().add(linkRecordins);
        hboxRecordins.getChildren().add(labelYear);

        HBox hboxPHS = new HBox();
        hboxPHS.alignmentProperty().setValue(Pos.CENTER);
        hboxPHS.getChildren().add(labelCreator);
        hboxPHS.getChildren().add(linkPHS);

        HBox hboxGitHub = new HBox();
        hboxGitHub.alignmentProperty().setValue(Pos.CENTER);
        hboxGitHub.getChildren().add(labelGitHub);
        hboxGitHub.getChildren().add(linkGitHub);

        HBox hboxRecordins2 = new HBox();
        hboxRecordins2.alignmentProperty().setValue(Pos.CENTER);
        hboxRecordins2.getChildren().add(new VBox(5, new Label(), labelRecordins1, labelRecordins2));
        hboxRecordins2.getChildren().add(imageViewrecordins);

        vboxInformationRoot = new VBox();
        vboxInformationRoot.alignmentProperty().setValue(Pos.CENTER);
        vboxInformationRoot.setSpacing(15);

        vboxInformationRoot.getChildren().add(imageView);
        vboxInformationRoot.getChildren().add(new HBox());
        vboxInformationRoot.getChildren().add(hboxText);
        vboxInformationRoot.getChildren().add(hboxRecordins);
        vboxInformationRoot.getChildren().add(hboxPHS);
        vboxInformationRoot.getChildren().add(hboxGitHub);
        vboxInformationRoot.getChildren().add(hboxRecordins2);

        vboxInformationRoot.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

    }

    private void fireSaveButton() {

        TreeItemNode selectedTreeItem = (TreeItemNode) treeView.getSelectionModel().getSelectedItem();

        if (selectedTreeItem == null) {
            selectedTreeItem = (TreeItemNode) treeView.getRoot();
        }

        if (checkFilledInformation()) {

            ImageView image;

            if (textFieldFileLocation.getText().toLowerCase().startsWith("ssh://")) {
                image = new ImageView(new Image("purple-document-ssh.png"));
            } else {
                image = new ImageView(new Image("blue-document-text.png"));
            }

            if (newTreeItemFlag || copyTreeItemFlag) {

                if (!selectedTreeItem.isNode()) {
                    selectedTreeItem = (TreeItemNode) selectedTreeItem.getParent();
                }
                if (copyTreeItemFlag) {
                    textFieldNodeName.setText(textFieldNodeName.getText() + " (Copy)");
                }

                InformationObject informationObject = new InformationObject(textFieldNodeName.getText(), textFieldNodeName.getText(), textFieldFileLocation.getText(), Integer.parseInt(textFieldBufferSize.getText()), Integer.parseInt(textFieldFrequency.getText()), displayColors.isSelected(), colorFileName);

                ApplicationContext.getInstance().put(textFieldNodeName.getText(), new WindowTextConsole(textFieldNodeName.getText(), informationObject));

                if (copyTreeItemFlag) {
                    String charset = (String) choiceBoxCharset.getSelectionModel().getSelectedItem();
                    String colorTemplate = (String) choiceBoxColorTemplate.getSelectionModel().getSelectedItem();
                    if (!charset.equals("Auto detect")) {
                        informationObject.setCharset(Charset.forName(charset));
                    }
                    informationObject.setColorConfiguration(new ColorConfiguration("etc/" + colorTemplate));
                }

                TreeItemNode newTreeItemNode = new TreeItemNode(informationObject, false);

                newTreeItemNode.setGraphic(image);

                if (selectedTreeItem.addNode(newTreeItemNode, treeView)) {
                    refreshInformationVBox(newTreeItemNode);
                    editFlag = false;
                    save.setDisable(true);
                    edit.setText("Edit");
                    edit.requestFocus();
                    gridPaneInformation.setDisable(true);
                    labelNodeName.setDisable(true);
                    textFieldNodeName.setDisable(true);
                    choiceBoxColorTemplate.setDisable(true);
                    choiceBoxCharset.setDisable(true);
                    labelFrequency.setDisable(true);
                    textFieldFrequency.setDisable(true);

                    logger.debug("File succesfully created: \"" + newTreeItemNode.getValue() + "\"");

                } else {

                    Alert alert = new Alert(AlertType.WARNING);
                    alert.setTitle("Warning Dialog");
                    alert.setHeaderText("Name already in use");
                    alert.setContentText("Name already in use: \"" + newTreeItemNode.getValue() + "\"");

                    alert.showAndWait();

                    logger.warning("Node name already used: \"" + newTreeItemNode.getValue() + "\"");
                }

            } else if (selectedTreeItem != null) {

                if (selectedTreeItem.isUniqNode(textFieldNodeName.getText()) || textFieldNodeName.getText().equals(selectedTreeItem.getInformationObject().getDisplayName())) {

                    String charset = (String) choiceBoxCharset.getSelectionModel().getSelectedItem();
                    String colorTemplate = (String) choiceBoxColorTemplate.getSelectionModel().getSelectedItem();

                    selectedTreeItem.setGraphic(image);

                    boolean charsetChanged = false;
                    int oldFrequency = selectedTreeItem.getInformationObject().getFrequency();

                    if (!charset.equals("Auto detect")) {

                        try {
                            if (selectedTreeItem.getInformationObject().getCharset() != null) {
                                if (!selectedTreeItem.getInformationObject().getCharset().name().equals(charset)) {
                                    charsetChanged = true;
                                }
                            } else {
                                charsetChanged = true;
                            }

                            selectedTreeItem.getInformationObject().setCharset(Charset.forName(charset));
                        } catch (Exception e) {
                        }
                    } else {
                        selectedTreeItem.getInformationObject().setCharset(null);
                    }
                    selectedTreeItem.getInformationObject().setFileColors("etc/" + colorTemplate);
                    selectedTreeItem.getInformationObject().setColorConfiguration(new ColorConfiguration("etc/" + colorTemplate));
                    selectedTreeItem.getInformationObject().setFilePath(textFieldFileLocation.getText());
                    selectedTreeItem.getInformationObject().setBufferSize(Integer.parseInt(textFieldBufferSize.getText()));
                    selectedTreeItem.getInformationObject().setFrequency(Integer.parseInt(textFieldFrequency.getText()));
                    selectedTreeItem.getInformationObject().setFullName(TreeItemNode.getFullPath(selectedTreeItem));
                    selectedTreeItem.setValue(textFieldNodeName.getText());
                    selectedTreeItem.getInformationObject().setDisplayColors(displayColors.isSelected());
                    selectedTreeItem.refreshNode(treeView);

                    if (charsetChanged) {

                        logger.debug("Charset Changed");
                        selectedTreeItem.stopTailSchedule();

                        try {
                            Thread.sleep(oldFrequency + 50);
                        } catch (InterruptedException ex) {
                        }

                        selectedTreeItem.getInformationObject().setLastFileLength(0);
                        selectedTreeItem.getInformationObject().setOffset(0);

                        selectedTreeItem.startTailSchedule();
                    }

                    editFlag = false;
                    save.setDisable(true);
                    edit.setText("Edit");
                    edit.requestFocus();
                    gridPaneInformation.setDisable(true);
                    labelNodeName.setDisable(true);
                    textFieldNodeName.setDisable(true);
                    choiceBoxColorTemplate.setDisable(true);
                    choiceBoxCharset.setDisable(true);
                    labelFrequency.setDisable(true);
                    textFieldFrequency.setDisable(true);

                    logger.debug("File succesfully updated: \"" + textFieldNodeName.getText() + "\"");
                } else {

                    Alert alert = new Alert(AlertType.WARNING);
                    alert.setTitle("Warning Dialog");
                    alert.setHeaderText("Name already in use");
                    alert.setContentText("Name already in use: \"" + textFieldNodeName.getText() + "\"");

                    alert.showAndWait();

                    logger.warning("Node name already used: \"" + textFieldNodeName.getText() + "\"");
                }
            }

            saveTreeToFile();
        } else {

            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Warning Dialog");
            alert.setHeaderText("Input information warning");
            alert.setContentText("Please, fill in information accordingly");

            alert.showAndWait();

        }

    }

    private void initMainWindow() {

        Button btOpenTree;
        ChoiceBox choiceBoxTreeFile = new ChoiceBox();
        Button btSaveTree;
        Button btAdd;
        Button btRemove;
        Button btColorChooser;
        Button btInsertLine;
        Button btOpenSavedfile;
        Button btHelp;

        btOpenTree = new Button();

        btOpenTree.setGraphic(new ImageView(new Image("open.png")));
        btOpenTree.setTooltip(new Tooltip("Open files tree"));
        btOpenTree.setPrefWidth(30);
        btOpenTree.setPrefHeight(30);
        btOpenTree.setOnAction(new EventHandler<ActionEvent>() {

                                   @Override
                                   public void handle(ActionEvent event) {

                                       FileChooser fileChooser = new FileChooser();
                                       Path etcFolder = Paths.get("etc");

                                       fileChooser.setInitialDirectory(etcFolder.toFile());
                                       fileChooser.setTitle("Open files tree");
                                       fileChooser.getExtensionFilters().addAll(
                                               new FileChooser.ExtensionFilter("Tree Configuration", "*.tree")
                                       );

                                       final File file = fileChooser.showOpenDialog(mainStage);

                                       if (file != null) {
                                           Path relative = Paths.get(etcFolder.toFile().getAbsolutePath()).relativize(Paths.get(file.getAbsolutePath()));

                                           logger.debug("file: etc/" + relative);
                                           treeFileName = "etc/" + relative;

                                           treePane.getChildren().remove(treeView);

                                           treeRoot = null;

                                           initTreeRoot();

                                           treeView = TreeViewBuilder.<String>create().root(treeRoot).build();

                                           treeView.setEditable(true);
                                           treeView.setMinSize(150, 200);

                                           treeView.setCellFactory(
                                                   new Callback<TreeView<String>, TreeCell<String>>() {
                                                       @Override
                                                       public TreeCell<String> call(TreeView<String> p) {
                                                           return new TreeCellImpl(p);
                                                       }
                                                   }
                                           );

                                           treePane.getChildren().add(treeView);
                                           initTreeNodes();
                                       }

                                       int selectedIndexTreeFile = 0;

                                       for (String item : (ObservableList<String>) choiceBoxTreeFile.getItems()) {

                                           String treeFileNameTmp = treeFileName;

                                           if (treeFileName.contains("/")) {
                                               treeFileNameTmp = treeFileNameTmp.substring(treeFileNameTmp.lastIndexOf("/") + 1);
                                           }

                                           if (item.equals(treeFileNameTmp)) {
                                               break;
                                           }
                                           selectedIndexTreeFile++;
                                       }

                                       if (selectedIndexTreeFile >= choiceBoxTreeFile.getItems().size()) {
                                           selectedIndexTreeFile = 0;
                                       }

                                       choiceBoxTreeFile.getSelectionModel().select(selectedIndexTreeFile);
                                   }
                               }
        );

        ArrayList<String> fileList = new ArrayList();
        InSideLog.getTreeFileList(Paths.get("etc").toFile(), fileList);

        choiceBoxTreeFile.setPrefHeight(30);
        choiceBoxTreeFile.setTooltip(new Tooltip("Switch files tree"));
        choiceBoxTreeFile.setItems(FXCollections.observableArrayList(fileList));
        int selectedIndexTreeFile = 0;

        for (String item : (ObservableList<String>) choiceBoxTreeFile.getItems()) {

            String treeFileNameTmp = treeFileName;

            if (treeFileName.contains("/")) {
                treeFileNameTmp = treeFileNameTmp.substring(treeFileNameTmp.lastIndexOf("/") + 1);
            }

            if (item.equals(treeFileNameTmp)) {
                break;
            }
            selectedIndexTreeFile++;
        }

        if (selectedIndexTreeFile >= choiceBoxTreeFile.getItems().size()) {
            selectedIndexTreeFile = 0;
        }

        choiceBoxTreeFile.getSelectionModel().select(selectedIndexTreeFile);

        choiceBoxTreeFile.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue ov, Number value, Number new_value) {

                if (new_value.intValue() != -1) {
                    logger.debug("Selected tree file: " + "etc/" + choiceBoxTreeFile.getItems().get((int) new_value));
                    treeFileName = "etc/" + choiceBoxTreeFile.getItems().get((int) new_value);
                }
                treePane.getChildren().remove(treeView);

                treeRoot = null;

                initTreeRoot();

                treeView = TreeViewBuilder.<String>create().root(treeRoot).build();

                treeView.setEditable(true);
                treeView.setMinSize(150, 200);

                treeView.setCellFactory(
                        new Callback<TreeView<String>, TreeCell<String>>() {
                            @Override
                            public TreeCell<String> call(TreeView<String> p) {
                                return new TreeCellImpl(p);
                            }
                        }
                );

                treePane.getChildren().add(treeView);
                initTreeNodes();
            }
        });

        btSaveTree = new Button();

        btSaveTree.setGraphic(new ImageView(new Image("disk-black.png")));
        btSaveTree.setTooltip(new Tooltip("Save files tree"));
        btSaveTree.setPrefWidth(30);
        btSaveTree.setPrefHeight(30);
        btSaveTree.setOnAction(new EventHandler<ActionEvent>() {

                                   @Override
                                   public void handle(ActionEvent event) {

                                       FileChooser fileChooser = new FileChooser();

                                       Path etcFolder = Paths.get("etc");

                                       fileChooser.setInitialDirectory(etcFolder.toFile());
                                       fileChooser.setTitle("Save files tree");
                                       fileChooser.getExtensionFilters().addAll(
                                               new FileChooser.ExtensionFilter("Tree Configuration", "*.tree")
                                       );

                                       File file = fileChooser.showSaveDialog(mainStage);

                                       if (file != null) {
                                           Path relative = Paths.get(etcFolder.toFile().getAbsolutePath()).relativize(Paths.get(file.getAbsolutePath()));
                                           treeFileName = "etc/" + relative;

                                           if (!treeFileName.endsWith(".tree")) {
                                               treeFileName += ".tree";
                                           }

                                           try {
                                               Files.deleteIfExists(relative);
                                           } catch (IOException e) {
                                               logger.error("Error saving files tree file: " + treeFileName, e);
                                           }

                                           saveTreeToFile();
                                       }

                                       ArrayList<String> fileList = new ArrayList();
                                       InSideLog.getTreeFileList(Paths.get("etc").toFile(), fileList);

                                       choiceBoxTreeFile.setItems(FXCollections.observableArrayList(fileList));
                                       int selectedIndexTreeFile = 0;

                                       for (String item : (ObservableList<String>) choiceBoxTreeFile.getItems()) {

                                           String treeFileNameTmp = treeFileName;

                                           if (treeFileName.contains("/")) {
                                               treeFileNameTmp = treeFileNameTmp.substring(treeFileNameTmp.lastIndexOf("/") + 1);
                                           }

                                           if (item.equals(treeFileNameTmp)) {
                                               break;
                                           }
                                           selectedIndexTreeFile++;
                                       }

                                       if (selectedIndexTreeFile >= choiceBoxTreeFile.getItems().size()) {
                                           selectedIndexTreeFile = 0;
                                       }

                                       choiceBoxTreeFile.getSelectionModel().select(selectedIndexTreeFile);
                                   }
                               }
        );

        btOpenSavedfile = new Button();

        btOpenSavedfile.setGraphic(new ImageView(new Image("blue-document-import.png")));
        btOpenSavedfile.setTooltip(new Tooltip("Open Saved File"));
        btOpenSavedfile.setPrefWidth(30);
        btOpenSavedfile.setPrefHeight(30);
        btOpenSavedfile.setOnAction(new EventHandler<ActionEvent>() {

                                        @Override
                                        public void handle(ActionEvent event) {
                                            final FileChooser fileChooser = new FileChooser();

                                            if (lastSelected != null) {
                                                fileChooser.setInitialDirectory(lastSelected.getParentFile());
                                            }

                                            final File selectedFile = fileChooser.showOpenDialog(mainStage);
                                            if (selectedFile != null) {
                                                lastSelected = selectedFile;
                                                openSavedFile(selectedFile.toString());
                                            }
                                        }
                                    }
        );

        btInsertLine = new Button();

        btInsertLine.setGraphic(new ImageView(new Image("line.png")));
        btInsertLine.setTooltip(new Tooltip("Insert breaking line"));
        btInsertLine.setPrefWidth(30);
        btInsertLine.setPrefHeight(30);
        btInsertLine.setOnAction(new EventHandler<ActionEvent>() {

                                     @Override
                                     public void handle(ActionEvent event) {

                                         ArrayList<TreeItemNode> nodes = new ArrayList();
                                         TreeItemNode.getRecursiveLeaf((TreeItemNode) treeView.getRoot(), nodes);

                                         for (TreeItemNode node : nodes) {

                                             node.getInformationObject().getWindowTextConsole().insertLine(node.getInformationObject().isDisplayColors());
                                         }
                                     }
                                 }
        );

        btAdd = new Button();

        btAdd.setGraphic(new ImageView(new Image("plus.png")));
        btAdd.setTooltip(new Tooltip("Add file"));
        btAdd.setPrefWidth(30);
        btAdd.setPrefHeight(30);
        btAdd.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {

                        newTreeItemFlag = true;

                        textFieldNodeName.setText("");
                        textFieldFileLocation.setText("");

                        if (ApplicationContext.getInstance().containsKey("tail.displayColor")) {
                            displayColors.setSelected(ApplicationContext.getInstance().getBoolean("tail.displayColor"));
                        } else {
                            displayColors.setSelected(true);
                        }

                        if (ApplicationContext.getInstance().containsKey("tail.bufferSize")) {
                            textFieldBufferSize.setText(ApplicationContext.getInstance().getString("tail.bufferSize"));
                        } else {
                            textFieldBufferSize.setText("100");
                        }

                        if (ApplicationContext.getInstance().containsKey("tail.frequencyInterval")) {
                            textFieldFrequency.setText(ApplicationContext.getInstance().getString("tail.frequencyInterval"));
                        } else {
                            textFieldBufferSize.setText("500");
                        }

                        metricOveralDefinition.setVisible(true);
                        gridPaneName.setDisable(false);
                        labelNodeName.setDisable(false);
                        textFieldNodeName.setDisable(false);

                        save.setVisible(true);
                        edit.setVisible(true);
                        save.setDisable(false);
                        edit.setDisable(false);
                        gridPaneInformation.setDisable(false);
                        labelNodeName.setDisable(false);
                        textFieldNodeName.setDisable(false);
                        labelFrequency.setDisable(false);
                        textFieldFrequency.setDisable(false);

                        edit.setText("Cancel");
                        stackPaneInformation.getChildren().clear();
                        stackPaneInformation.getChildren().add(vboxInformationFile);
                    }
                }
        );

        btRemove = new Button();

        btRemove.setGraphic(new ImageView(new Image("minus.png")));
        btRemove.setTooltip(new Tooltip("Remove File"));
        btRemove.setPrefWidth(30);
        btRemove.setPrefHeight(30);
        btRemove.setOnAction(new EventHandler<ActionEvent>() {

                                 @Override
                                 public void handle(ActionEvent event) {

                                     TreeItemNode selectedTreeItem = (TreeItemNode) treeView.getSelectionModel().getSelectedItem();
                                     if (selectedTreeItem != null) {
                                         Alert alert = new Alert(AlertType.CONFIRMATION);
                                         alert.setTitle("Remove confirmation");
                                         alert.setHeaderText("Remove confirmation");
                                         alert.setContentText("Would you like to remove node: " + selectedTreeItem.getInformationObject().getDisplayName());

                                         Optional<ButtonType> result = alert.showAndWait();
                                         if (result.get() == ButtonType.OK) {

                                             refreshInformationVBox((TreeItemNode) selectedTreeItem.getParent());
                                             selectedTreeItem.hideTextconsole();
                                             selectedTreeItem.stopTailSchedule();
                                             selectedTreeItem.removeNode(treeView);

                                             logger.debug("File succesfully removed: \"" + selectedTreeItem.getInformationObject().getDisplayName() + "\"");
                                             saveTreeToFile();
                                         }

                                     }
                                 }
                             }
        );

        edit = new Button();

        edit.setText("Edit");
        edit.setPrefWidth(80);

        edit.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent e) {

                        if (newTreeItemFlag) {

                            save.setVisible(false);
                            edit.setVisible(false);
                            metricOveralDefinition.setVisible(false);

                            refreshInformationVBox((TreeItemNode) treeView.getSelectionModel().getSelectedItem());
                            edit.setText("Edit");
                        } else if (editFlag) {
                            editFlag = !editFlag;
                            save.setDisable(true);
                            edit.setText("Edit");
                            edit.requestFocus();
                            gridPaneInformation.setDisable(true);
                            labelNodeName.setDisable(true);
                            textFieldNodeName.setDisable(true);
                            choiceBoxColorTemplate.setDisable(true);
                            choiceBoxCharset.setDisable(true);
                            labelFrequency.setDisable(true);
                            textFieldFrequency.setDisable(true);
                        } else {
                            editFlag = !editFlag;
                            save.setDisable(false);
                            labelNodeName.setDisable(false);
                            textFieldNodeName.setDisable(false);
                            gridPaneInformation.setDisable(false);
                            choiceBoxColorTemplate.setDisable(false);
                            choiceBoxCharset.setDisable(false);
                            labelFrequency.setDisable(false);
                            textFieldFrequency.setDisable(false);
                            edit.setText("Cancel");
                            save.requestFocus();
                        }
                    }
                }
        );

        save = new Button();

        save.setText("Save");
        save.setPrefWidth(80);

        save.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent e) {

                        // button.fire() Not working on all Windows 7 with all JRE...,
                        fireSaveButton();
                    }
                }
        );

        btColorChooser = new Button();

        btColorChooser.setGraphic(new ImageView(new Image("color-swatch.png")));
        btColorChooser.setTooltip(new Tooltip("Choose default colors"));
        btColorChooser.setPrefWidth(30);
        btColorChooser.setPrefHeight(30);
        btColorChooser.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {

                        rootInformationObject.setColorConfiguration((ColorConfiguration) ApplicationContext.getInstance().get("colorDefaultConfiguration"));

                        ColorChooserWindow colorChooserWindow = new ColorChooserWindow(rootInformationObject, colorFileName);
                        colorChooserWindow.start(new Stage());
                    }
                }
        );

        btHelp = new Button();

        btHelp.setGraphic(new ImageView(new Image("question.png")));
        btHelp.setTooltip(new Tooltip("Read user documentation"));
        btHelp.setPrefWidth(30);
        btHelp.setPrefHeight(30);
        btHelp.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {

                        if (Files.exists(Paths.get("UserManual.pdf"))) {

                            String osName = System.getProperty("os.name").toLowerCase();
                            if (!osName.contains("nix") && !osName.contains("nux") && !osName.contains("aix")) {
                                if (Desktop.isDesktopSupported()) {
                                    try {
                                        File myFile = new File("UserManual.pdf");
                                        Desktop.getDesktop().open(myFile);
                                    } catch (IOException ex) {
                                        logger.info("No PDF reader found");
                                        getHostServices().showDocument("https://github.com/pschweitz/insidelog");
                                    }
                                }
                            } else {
                                getHostServices().showDocument("https://github.com/pschweitz/insidelog");
                            }
                        } else {
                            getHostServices().showDocument("https://github.com/pschweitz/insidelog");
                        }
                    }
                }
        );

        toolBar = new HBox();

        toolBar.setSpacing(15);
        toolBar.setPadding(new Insets(13, 16, 13, 16));

        toolBar.getChildren().add(btOpenTree);
        toolBar.getChildren().add(choiceBoxTreeFile);
        toolBar.getChildren().add(btSaveTree);
        toolBar.getChildren().add(btAdd);
        toolBar.getChildren().add(btRemove);
        toolBar.getChildren().add(btColorChooser);
        toolBar.getChildren().add(btInsertLine);
        toolBar.getChildren().add(btOpenSavedfile);
        toolBar.getChildren().add(btHelp);

        Pane spacer1 = new Pane();

        HBox.setHgrow(spacer1, Priority.ALWAYS);

        spacer1.setMinWidth(Region.USE_PREF_SIZE);

        Pane spacer2 = new Pane();

        HBox.setHgrow(spacer2, Priority.ALWAYS);

        spacer2.setMinWidth(Region.USE_PREF_SIZE);

        Pane spacer3 = new Pane();

        HBox.setHgrow(spacer3, Priority.ALWAYS);

        spacer3.setMinWidth(Region.USE_PREF_SIZE);

        HBox paneControl = new HBox();

        paneControl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        paneControl.setAlignment(Pos.CENTER);

        paneControl.getChildren().add(spacer1);
        paneControl.getChildren().add(edit);
        paneControl.getChildren().add(spacer2);
        paneControl.getChildren().add(save);
        paneControl.getChildren().add(spacer3);

        labelNodeName = new Label("Name:");

        gridPaneName = new GridPane();

        gridPaneName.setPadding(new Insets(0, 10, 10, 10));

        ColumnConstraints column1 = new ColumnConstraints();

        column1.setPercentWidth(40);
        column1.setHalignment(HPos.RIGHT);
        ColumnConstraints column2 = new ColumnConstraints();

        column2.setPercentWidth(60);
        gridPaneName.getColumnConstraints().addAll(column1, column2);

        gridPaneName.setHgap(5);
        gridPaneName.setVgap(10);

        gridPaneName.add(paneControl, 0, 1, 2, 1);
        gridPaneName.add(labelNodeName, 0, 2);
        gridPaneName.add(textFieldNodeName, 1, 2);

        metricOveralDefinition = new TitledPane();

        metricOveralDefinition.setText("Logfile Name");
        metricOveralDefinition.setMinWidth(150);
        metricOveralDefinition.setContent(gridPaneName);
        Node content = metricOveralDefinition.getContent();

        content.setStyle("-fx-background-color: #dce6e6;");

        StackPane stackPaneMetricOveralDefinition = new StackPane();

        stackPaneMetricOveralDefinition.setPadding(new Insets(5, 10, 5, 10));
        stackPaneMetricOveralDefinition.getChildren().add(metricOveralDefinition);
        stackPaneMetricOveralDefinition.setMinWidth(150);
        stackPaneMetricOveralDefinition.setPadding(new Insets(5, 10, 5, 10));

        stackPaneInformation.getChildren().add(vboxInformationRoot);
        stackPaneInformation.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        stackPaneInformation.setPadding(new Insets(5, 10, 5, 10));
        stackPaneInformation.setMinSize(150, 200);

        VBox vboxInformation = new VBox();

        vboxInformation.getChildren().add(stackPaneMetricOveralDefinition);
        vboxInformation.getChildren().add(stackPaneInformation);

        initTreeRoot();

        treePane = new StackPane();

        treeView = TreeViewBuilder.<String>create().root(treeRoot).build();

        treeView.setEditable(true);
        treeView.setMinSize(150, 200);

        treeView.setCellFactory(
                new Callback<TreeView<String>, TreeCell<String>>() {
                    @Override
                    public TreeCell<String> call(TreeView<String> p) {
                        return new TreeCellImpl(p);
                    }
                }
        );

        treePane.getChildren().add(treeView);

        splitPaneCenter = new SplitPane();

        splitPaneCenter.setStyle("-fx-background-color: #dce6ec;-fx-padding: 7;-fx-background-insets: 0, 7;-fx-box-border: transparent;");
        splitPaneCenter.getItems().add(treePane);
        splitPaneCenter.getItems().add(vboxInformation);
        splitPaneCenter.setDividerPosition(0, 0.4);

    }

    private void initTreeRoot() {

        if (treeRoot == null) {
            rootInformationObject = new InformationObject("in'side log", "Root Node", null, 0, 0, false, colorFileName);
            treeRoot = new TreeItemNode(rootInformationObject, true);
            treeRoot.setGraphic(new ImageView(new Image("direction.png")));
            treeRoot.setExpanded(true);
            refreshInformationVBox(treeRoot);
        }
    }

    public void initTreeNodes() {

        if (Files.exists(Paths.get(treeFileName))) {

            File file = new File(treeFileName);
            DataInputStream in = null;
            BufferedReader br = null;

            try {
                FileInputStream fstream = new FileInputStream(file);
                InputStreamReader is = new InputStreamReader(fstream, Charset.forName("UTF-8"));
                br = new BufferedReader(is);

                StringTokenizer stringTokenizer;

                String line = "";
                String displayName = null;
                String fullName = null;
                String filePath = null;
                int lastLines = 10;
                int frequency = 500;
                boolean displayColors = false;
                String charsetName = "";
                String fileColors = colorFileName;

                while (line != null) {
                    if (!line.equals("") && !line.equals("\r")) {
                        charsetName = "";
                        fileColors = colorFileName;

                        stringTokenizer = new StringTokenizer(line, ";\"\"");

                        int counter = 0;
                        while (stringTokenizer.hasMoreElements()) {

                            String token = stringTokenizer.nextToken();

                            switch (counter) {

                                case 0:

                                    displayName = token;
                                    break;

                                case 1:

                                    fullName = token;
                                    break;

                                case 2:

                                    filePath = token;
                                    break;

                                case 3:

                                    lastLines = Integer.parseInt(token);
                                    break;

                                case 4:

                                    frequency = Integer.parseInt(token);
                                    break;

                                case 5:

                                    displayColors = Boolean.parseBoolean(token);
                                    break;

                                case 6:

                                    charsetName = token;
                                    break;

                                case 7:

                                    fileColors = token;
                                    break;
                            }
                            counter++;
                        }

                        TreeItemNode parent = TreeItemNode.createSubGroups(treeView, fullName);
                        InformationObject informationObject = new InformationObject(displayName, fullName, filePath, lastLines, frequency, displayColors, fileColors);

                        try {
                            informationObject.setCharset(Charset.forName(charsetName));
                        } catch (Exception e) {
                        }

                        ApplicationContext.getInstance().put(fullName, new WindowTextConsole(fullName, informationObject));

                        TreeItemNode newTreeItemNode = new TreeItemNode(informationObject, false);

                        ImageView image;

                        if (informationObject.getFilePath().toLowerCase().startsWith("ssh://")) {
                            image = new ImageView(new Image("purple-document-ssh.png"));
                        } else {
                            image = new ImageView(new Image("blue-document-text.png"));
                        }

                        newTreeItemNode.setGraphic(image);
                        if (parent.addNode(newTreeItemNode, treeView)) {
                            refreshInformationVBox(newTreeItemNode);
                        } else {
                            logger.warning("Node name already used");
                        }
                    }
                    line = br.readLine();
                }

                br.close();
            } catch (IOException e) {
                logger.error("Error opening tree file: " + treeFileName, e);
            }

        }
        refreshInformationVBox(treeRoot);
    }

    public static void updateInformationObjetcs(ColorConfiguration colorConfiguration) {
        TreeItemNode.refreshColorConfiguration(instance.treeRoot, colorConfiguration);
    }

    public static void saveTreeToFile() {

        BufferedWriter bw_treeView = null;

        Path filePath = Paths.get(treeFileName);

        logger.trace("Saving tree file to: " + treeFileName);

        try {
            Files.deleteIfExists(filePath);

            if (!Files.exists(Paths.get("etc"))) {
                Files.createDirectory(Paths.get("etc"));
            }
            Files.createFile(filePath);

            bw_treeView = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(treeFileName), Charset.forName("UTF-8")));

        } catch (IOException e) {
            logger.error("Error saving tree file: " + treeFileName, e);
        }

        ArrayList<TreeItemNode> nodes = new ArrayList();

        TreeItemNode.getRecursiveLeaf((TreeItemNode) treeView.getRoot(), nodes);

        for (TreeItemNode node : nodes) {

            try {
                String charsetName = "";
                if (node.getInformationObject().getCharset() != null) {
                    charsetName = node.getInformationObject().getCharset().name();
                } else {
                    charsetName = "Auto detect";
                }

                logger.trace("node.getInformationObject().getDisplayName(): " + node.getInformationObject().getDisplayName());
                logger.trace("node.getInformationObject().getFullName(): " + node.getInformationObject().getFullName());
                logger.trace("node.getInformationObject().getFilePath().toString(): " + node.getInformationObject().getFilePath().toString());
                logger.trace("node.getInformationObject().getBufferSize(): " + node.getInformationObject().getBufferSize());
                logger.trace("node.getInformationObject().getFrequency(): " + node.getInformationObject().getFrequency());
                logger.trace("node.getInformationObject().isDisplayColors(): " + node.getInformationObject().isDisplayColors());
                logger.trace("node.getInformationObject().getCharset().name()): " + charsetName);
                logger.trace("node.getInformationObject().getFileColors(): " + node.getInformationObject().getFileColors());
                logger.trace("----------------------------------------------------------------------------------------");

                bw_treeView.write("\"" + node.getInformationObject().getDisplayName() + "\";\"" + node.getInformationObject().getFullName() + "\";\"" + node.getInformationObject().getFilePath().toString() + "\";\"" + node.getInformationObject().getBufferSize() + "\";\"" + node.getInformationObject().getFrequency() + "\";\"" + node.getInformationObject().isDisplayColors() + "\";\"" + charsetName + "\";\"" + node.getInformationObject().getFileColors() + "\"");
                bw_treeView.newLine();
                bw_treeView.flush();
            } catch (IOException e) {
                logger.error("Error saving tree file: " + treeFileName, e);
            }
        }

        try {
            bw_treeView.close();

        } catch (IOException e) {
            logger.error("Error saving tree file: " + treeFileName, e);
        }
    }

    private void initMainStage(Stage mainStage) {
        this.mainStage = mainStage;
        mainStage.setOnCloseRequest(new EventHandler<WindowEvent>() {

                                        @Override
                                        public void handle(WindowEvent event) {

                                            if (!Files.exists(Paths.get(ApplicationContext.getPropertiesFilename()))) {
                                                try {
                                                    if (!Files.exists(Paths.get("etc"))) {
                                                        Files.createDirectory(Paths.get("etc"));
                                                    }
                                                    ApplicationContext.getInstance().storeProperties();
                                                } catch (Exception e) {

                                                }
                                            }
                                            System.exit(0);
                                            event.consume();
                                        }
                                    }
        );

        BorderPane borderPaneRoot = new BorderPane();

        borderPaneRoot.setTop(toolBar);

        borderPaneRoot.setCenter(splitPaneCenter);

        borderPaneRoot.setStyle("-fx-base: #dce6e0");

        Scene scene = new Scene(borderPaneRoot, 600, 350);
        scene.getStylesheets().add("main.css");

        scene.setFill(Color.web("#dce6e8"));
        mainStage.setScene(scene);

        mainStage.setResizable(false);
        mainStage.setTitle("in'side log");
        mainStage.setMinHeight(550);
        mainStage.setMinWidth(360);

        mainStage.setWidth(650);
        mainStage.setHeight(470);
        mainStage.setResizable(true);

        mainStage.getIcons().add(new Image("tail_small_logo.png"));

        if (showMainStage) {
            mainStage.show();
        }
    }

    public boolean checkFilledInformation() {
        boolean result = true;

        if (textFieldNodeName.getText().equals("")) {
            return false;
        }
        if (textFieldFileLocation.getText().equals("")) {
            return false;
        }

        try {
            Integer.parseInt(textFieldBufferSize.getText());
        } catch (Exception e) {
            return false;
        }

        try {
            Integer.parseInt(textFieldFrequency.getText());
        } catch (Exception e) {
            return false;
        }

        return result;
    }

    public void refreshInformationVBox(final TreeItemNode treeItemNode) {

        editFlag = false;
        stackPaneInformation.getChildren().clear();

        if (treeItemNode == null || treeItemNode.getParent() == null) {
            metricOveralDefinition.setVisible(false);
            stackPaneInformation.getChildren().add(vboxInformationRoot);

        } else if (treeItemNode.isNode()) {

            metricOveralDefinition.setVisible(false);
            gridPaneName.setDisable(true);

            save.setVisible(false);
            edit.setVisible(false);
            textFieldNodeName.setText(treeItemNode.getInformationObject().getDisplayName());
            stackPaneInformation.getChildren().add(vboxInformationRoot);

        } else {

            metricOveralDefinition.setVisible(true);
            gridPaneName.setDisable(false);
            labelNodeName.setDisable(true);
            textFieldNodeName.setDisable(true);
            gridPaneInformation.setDisable(true);

            save.setVisible(true);
            edit.setVisible(true);
            save.setDisable(true);
            edit.setDisable(false);
            edit.setText("Edit");
            edit.requestFocus();

            labelFrequency.setDisable(true);
            textFieldFrequency.setDisable(true);

            newTreeItemFlag = false;

            textFieldFileLocation.setText(treeItemNode.getInformationObject().getFilePath().toString());
            textFieldNodeName.setText(treeItemNode.getInformationObject().getDisplayName());
            textFieldBufferSize.setText(String.valueOf(treeItemNode.getInformationObject().getBufferSize()));
            displayColors.setSelected(treeItemNode.getInformationObject().isDisplayColors());
            textFieldFrequency.setText(String.valueOf(treeItemNode.getInformationObject().getFrequency()));

            ArrayList<String> fileList = new ArrayList();
            //fileList.add("default.cfg");
            InSideLog.getConfigurationFileList(Paths.get("etc").toFile(), fileList);

            choiceBoxColorTemplate.setItems(FXCollections.observableArrayList(fileList));
            int selectedIndexColor = 0;

            for (String item : (ObservableList<String>) choiceBoxColorTemplate.getItems()) {
                if (item.equals(treeItemNode.getInformationObject().getColorConfiguration().templateName)) {
                    break;
                }
                selectedIndexColor++;
            }

            if (selectedIndexColor >= choiceBoxColorTemplate.getItems().size()) {
                selectedIndexColor = 0;
            }

            choiceBoxColorTemplate.getSelectionModel().select(selectedIndexColor);

            ArrayList<String> charsetList = new ArrayList();
            charsetList.add("Auto detect");
            charsetList.addAll(Charset.availableCharsets().keySet());

            choiceBoxCharset.setItems(FXCollections.observableArrayList(charsetList));
            int selectedIndexCharset = 0;
            for (String item : (ObservableList<String>) choiceBoxCharset.getItems()) {

                if (treeItemNode.getInformationObject().getCharset() != null) {
                    if (item.equals(treeItemNode.getInformationObject().getCharset().name())) {
                        break;
                    }
                    selectedIndexCharset++;
                } else {
                    break;
                }

            }

            if (selectedIndexCharset >= choiceBoxCharset.getItems().size()) {
                selectedIndexCharset = 0;
            }

            choiceBoxCharset.getSelectionModel().select(selectedIndexCharset);

            stackPaneInformation.getChildren().add(vboxInformationFile);
        }
    }

    public static void main(String[] args) {

        InSideLog.args = args;

        launch(args);

    }

    public static void getConfigurationFileList(File directory, ArrayList<String> files) {

        // get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile() && file.getName().endsWith(".cfg")) {
                files.add(file.getName());
            } else if (file.isDirectory()) {
                getConfigurationFileList(file, files);
            }
        }
    }

    public static void getTreeFileList(File directory, ArrayList<String> files) {

        // get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile() && file.getName().endsWith(".tree")) {
                files.add(file.getName());
            } else if (file.isDirectory()) {
                getConfigurationFileList(file, files);
            }
        }
    }

    private final class TreeCellImpl extends TreeCell<String> {

        private ContextMenu addMenu = null;

        private MenuItem addMenuItemCleanStart = new MenuItem("Clean/Start");
        private MenuItem addMenuItemStop = new MenuItem("Stop");
        private MenuItem addMenuItemRename = new MenuItem("Rename");
        private MenuItem addMenuItemGroup = new MenuItem("Add subgroup");
        private MenuItem addMenuCopy = new MenuItem("Copy");
        private MenuItem addMenuPaste = new MenuItem("Paste");
        private SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();
        private SeparatorMenuItem separatorMenuItem2 = new SeparatorMenuItem();

        private final Label message = new Label();
        private final TextField subGoupName = new TextField();
        private Button ok = new Button("ok");
        private Button cancel = new Button("cancel");

        private VBox popUpVBox = new VBox();
        private HBox popUpHBox = new HBox();

        private Stage renameStage;

        boolean newGroupFlag = false;

        private String item;
        private TreeView<String> parentTree;

        public TreeCellImpl(final TreeView<String> parentTree) {

            this.parentTree = parentTree;
            setOnDragDetected(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    logger.trace("Drag detected on " + item);
                    if (item == null) {
                        return;
                    }
                    Dragboard dragBoard = startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.put(DataFormat.PLAIN_TEXT, item.toString());
                    dragBoard.setContent(content);
                    event.consume();
                }
            });
            setOnDragDone(new EventHandler<DragEvent>() {
                @Override
                public void handle(DragEvent dragEvent) {
                    logger.trace("Drag done on " + item);

                    dragEvent.consume();
                }
            });

            setOnDragOver(new EventHandler<DragEvent>() {
                @Override
                public void handle(DragEvent dragEvent) {
                    logger.trace("Drag over on " + item);
                    treeView.getSelectionModel().select(getTreeItem());
                    if (dragEvent.getDragboard().hasString()) {
                        String valueToMove = dragEvent.getDragboard().getString();
                        if (valueToMove != item) {

                            dragEvent.acceptTransferModes(TransferMode.MOVE);

                        }
                    }
                    dragEvent.consume();
                }
            });

            setOnDragDropped(new EventHandler<DragEvent>() {
                @Override
                public void handle(DragEvent dragEvent) {
                    logger.trace("Drag dropped on " + item);
                    String valueToMove = dragEvent.getDragboard().getString();
                    TreeItemNode itemToMove = (TreeItemNode) search(parentTree.getRoot(), valueToMove);
                    TreeItemNode newParent = (TreeItemNode) search(parentTree.getRoot(), item);

                    if (newParent.isNode()) {
                        if (newParent.isUniqChild(itemToMove.getValue())) {

                            if (!newParent.isChild(itemToMove)) {
                                itemToMove.getParent().getChildren().remove(itemToMove);
                                newParent.addNode(itemToMove, treeView);

                                String fullname = TreeItemNode.getFullPath(itemToMove);
                                itemToMove.getInformationObject().setFullName(fullname);
                                itemToMove.getInformationObject().getWindowTextConsole().setName(fullname);

                                ArrayList<TreeItemNode> nodes = new ArrayList();

                                TreeItemNode.getRecursiveLeaf(itemToMove, nodes);

                                for (TreeItemNode node : nodes) {
                                    node.getInformationObject().setFullName(TreeItemNode.getFullPath(node));
                                    node.getInformationObject().getWindowTextConsole().setName(TreeItemNode.getFullPath(node));
                                }

                                saveTreeToFile();
                            } else {
                                treeView.getSelectionModel().select(itemToMove);

                                Alert alert = new Alert(AlertType.WARNING);
                                alert.setTitle("Warning Dialog");
                                alert.setHeaderText("You cant move a node into one of its child");
                                alert.setContentText("You cant move a node into one of its child");

                                alert.showAndWait();

                                logger.warning("You can't move a node into one of its child");
                            }
                        } else {
                            treeView.getSelectionModel().select(itemToMove);

                            Alert alert = new Alert(AlertType.WARNING);
                            alert.setTitle("Warning Dialog");
                            alert.setHeaderText("Name already in use");
                            alert.setContentText("Name already in use: \"" + itemToMove.getValue() + "\"");

                            alert.showAndWait();
                            logger.warning("Node name already used: " + itemToMove.getValue());
                        }
                    }

                    dragEvent.consume();
                }
            });

            message.setText("Group name:");
            ok = new Button("ok");
            cancel = new Button("cancel");

            ok.setPrefWidth(75);
            cancel.setPrefWidth(75);
            subGoupName.setMaxWidth(170);

            popUpHBox.getChildren().add(cancel);
            popUpHBox.getChildren().add(ok);
            popUpHBox.alignmentProperty().setValue(Pos.CENTER);
            popUpHBox.setSpacing(15);

            popUpVBox.getChildren().add(message);
            popUpVBox.getChildren().add(subGoupName);
            popUpVBox.getChildren().add(popUpHBox);
            popUpVBox.setPrefSize(200, 100);
            popUpVBox.alignmentProperty().setValue(Pos.CENTER);
            popUpVBox.setSpacing(15);

            Scene renameScene = new Scene(popUpVBox);

            renameStage = new Stage();
            renameScene.setFill(Color.web("#dce6ec"));

            renameStage.setTitle("Group name");
            renameStage.setScene(renameScene);

            renameStage.setResizable(false);
            subGoupName.setOnKeyReleased(new EventHandler<KeyEvent>() {

                @Override
                public void handle(KeyEvent t) {

                    if (t.getCode() == KeyCode.ENTER) {

                        if (!newGroupFlag) {

                            logger.debug("New name: " + subGoupName.getText());
                            if (((TreeItemNode) getTreeItem()).isUniqNode(subGoupName.getText())) {
                                commitEdit(subGoupName.getText());
                                ((TreeItemNode) getTreeItem()).refreshNode(treeView);
                                refreshInformationVBox((TreeItemNode) getTreeItem());
                            } else {
                                cancelEdit();

                                Alert alert = new Alert(AlertType.WARNING);
                                alert.setTitle("Warning Dialog");
                                alert.setHeaderText("Name already in use");
                                alert.setContentText("Name already in use: \"" + subGoupName.getText() + "\"");

                                alert.showAndWait();

                                logger.warning("Node name already used: " + subGoupName.getText());
                            }

                        } else {

                            TreeItemNode newNode = new TreeItemNode(new InformationObject(subGoupName.getText(), subGoupName.getText(), null, 0, 0, false, colorFileName), true);
                            newNode.setGraphic(new ImageView(new Image("folder-horizontal.png")));

                            if (((TreeItemNode) getTreeItem()).addNode(newNode, treeView)) {
                                refreshInformationVBox(newNode);

                            } else {

                                Alert alert = new Alert(AlertType.WARNING);
                                alert.setTitle("Warning Dialog");
                                alert.setHeaderText("Name already in use");
                                alert.setContentText("Name already in use: \"" + newNode.getValue() + "\"");

                                alert.showAndWait();

                                logger.warning("Node name already used: " + newNode.getValue());
                            }
                        }
                        renameStage.hide();
                        saveTreeToFile();

                    } else if (t.getCode() == KeyCode.ESCAPE) {

                        renameStage.hide();
                        cancelEdit();
                    }
                }
            });

            ok.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent t) {

                    if (!newGroupFlag) {

                        logger.debug("New name: " + subGoupName.getText());
                        if (((TreeItemNode) getTreeItem()).isUniqNode(subGoupName.getText())) {
                            commitEdit(subGoupName.getText());

                            ((TreeItemNode) getTreeItem()).refreshNode(treeView);
                            refreshInformationVBox((TreeItemNode) getTreeItem());
                        } else {
                            cancelEdit();

                            Alert alert = new Alert(AlertType.WARNING);
                            alert.setTitle("Warning Dialog");
                            alert.setHeaderText("Name already in use");
                            alert.setContentText("Name already in use: \"" + subGoupName.getText() + "\"");

                            alert.showAndWait();

                            logger.warning("Node name already used: " + subGoupName.getText());
                        }

                    } else {

                        TreeItemNode newNode = new TreeItemNode(new InformationObject(subGoupName.getText(), subGoupName.getText(), null, 0, 0, false, colorFileName), true);
                        newNode.setGraphic(new ImageView(new Image("folder-horizontal.png")));
                        if (((TreeItemNode) getTreeItem()).addNode(newNode, treeView)) {
                            refreshInformationVBox(newNode);

                        } else {
                            Alert alert = new Alert(AlertType.WARNING);
                            alert.setTitle("Warning Dialog");
                            alert.setHeaderText("Name already in use");
                            alert.setContentText("Name already in use: \"" + subGoupName.getText() + "\"");

                            alert.showAndWait();

                            logger.warning("Node name already used: " + subGoupName.getText());
                        }
                    }
                    renameStage.hide();
                    saveTreeToFile();
                }
            });

            cancel.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent t) {
                    renameStage.hide();
                }
            });

            addMenuItemCleanStart.setOnAction(new EventHandler() {
                public void handle(Event t) {
                    cleanStart();
                }
            });

            addMenuItemStop.setOnAction(new EventHandler() {
                public void handle(Event t) {
                    stop();
                }
            });
            addMenuItemRename.setOnAction(new EventHandler() {
                public void handle(Event t) {
                    editName();
                }
            });
            addMenuItemGroup.setOnAction(new EventHandler() {
                public void handle(Event t) {
                    newGroup();
                }
            });
            addMenuCopy.setOnAction(new EventHandler() {
                public void handle(Event t) {
                    copiedTreeItemNode = (TreeItemNode) getTreeItem();
                    addMenuPaste.setDisable(false);
                    logger.debug("Copy");
                }
            });
            addMenuPaste.setOnAction(new EventHandler() {
                public void handle(Event t) {
                    if (copiedTreeItemNode != null) {
                        refreshInformationVBox(copiedTreeItemNode);
                        copyTreeItemFlag = true;

                        logger.debug("Firesave");
                        fireSaveButton();

                        copyTreeItemFlag = false;
                    }
                    logger.debug("Paste");
                }
            });

            addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {

                    if (getTreeItem() != null) {

                        if (event.getClickCount() > 1) {

                            if (!((TreeItemNode) getTreeItem()).isNode()) {
                                ((TreeItemNode) getTreeItem()).stopTailSchedule();

                                InformationObject informationObject = ((TreeItemNode) getTreeItem()).getInformationObject();
                                if (ApplicationContext.getInstance().containsKey(informationObject.getFullName())) {

                                    ((TreeItemNode) getTreeItem()).getInformationObject().getWindowTextConsole().clear();
                                    ((TreeItemNode) getTreeItem()).getInformationObject().getWindowTextConsole().setIsRunning(true);

                                } else {
                                    ApplicationContext.getInstance().put(informationObject.getFullName(), new WindowTextConsole(informationObject.getFullName(), informationObject));
                                }

                                ((TreeItemNode) getTreeItem()).showTextconsole();
                                ((TreeItemNode) getTreeItem()).startTailSchedule();
                            }
                        }
                        refreshInformationVBox((TreeItemNode) getTreeItem());
                    }
                }
            });

        }

        private TreeItem<String> search(final TreeItem<String> currentNode, final String valueToSearch) {
            TreeItem<String> result = null;
            if (TreeItemNode.getFullPath((TreeItemNode) currentNode).equals(valueToSearch)) {
                result = currentNode;
            } else if (!currentNode.isLeaf()) {
                for (TreeItem<String> child : currentNode.getChildren()) {
                    result = search(child, valueToSearch);
                    if (result != null) {
                        break;
                    }
                }
            }
            return result;
        }

        private void cleanStart() {

            TreeItemNode selectedTreeItem = (TreeItemNode) getTreeItem();

            if (selectedTreeItem != null) {

                InformationObject informationObject = selectedTreeItem.getInformationObject();
                if (ApplicationContext.getInstance().containsKey(informationObject.getFullName())) {

                    selectedTreeItem.getInformationObject().getWindowTextConsole().clear();
                    selectedTreeItem.getInformationObject().getWindowTextConsole().setIsRunning(true);
                } else {
                    ApplicationContext.getInstance().put(informationObject.getFullName(), new WindowTextConsole(informationObject.getFullName(), informationObject));
                }
                selectedTreeItem.startTailSchedule();
                selectedTreeItem.showTextconsole();
            }
        }

        private void stop() {
            TreeItemNode selectedTreeItem = (TreeItemNode) getTreeItem();

            if (selectedTreeItem != null) {
                InformationObject informationObject = selectedTreeItem.getInformationObject();
                selectedTreeItem.stopTailSchedule();
                informationObject.setLastFileLength(0);
                informationObject.setOffset(0);
            }
        }

        private void editName() {

            if (getTreeItem().getParent() != null) {
                startEdit();
                subGoupName.setText(getString());
                subGoupName.requestFocus();
                renameStage.show();
            }
        }

        private void newGroup() {

            newGroupFlag = true;
            subGoupName.requestFocus();
            renameStage.show();
        }

        @Override
        public void startEdit() {
            super.startEdit();

        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();

            setText(getItem());
            setGraphic(getTreeItem().getGraphic());
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (getTreeItem() != null) {
                this.item = TreeItemNode.getFullPath((TreeItemNode) getTreeItem());
            }
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing()) {

            } else {
                setText(getString());
                setGraphic(getTreeItem().getGraphic());

                addMenu = new ContextMenu();

                if (((TreeItemNode) getTreeItem()).isFile()) {

                    if (!addMenu.getItems().contains(addMenuItemCleanStart)) {
                        addMenu.getItems().add(addMenuItemCleanStart);
                    }

                    if (!addMenu.getItems().contains(addMenuItemStop)) {
                        addMenu.getItems().add(addMenuItemStop);
                        addMenu.getItems().add(separatorMenuItem);
                    }

                    if (!addMenu.getItems().contains(addMenuCopy)) {
                        addMenu.getItems().add(addMenuCopy);
                    }
                }
                if (!addMenu.getItems().contains(addMenuPaste)) {

                    //                addMenuPaste.setDisable(true);
                    addMenu.getItems().add(addMenuPaste);
                    addMenu.getItems().add(separatorMenuItem2);
                }
                if (!addMenu.getItems().contains(addMenuItemRename)) {
                    addMenu.getItems().add(addMenuItemRename);
                }
                if (((TreeItemNode) getTreeItem()).isNode()) {

                    if (!addMenu.getItems().contains(addMenuItemGroup)) {
                        addMenu.getItems().add(separatorMenuItem);
                        addMenu.getItems().add(addMenuItemGroup);
                    }
                }
                setContextMenu(addMenu);
            }
        }

        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }

    }
}
