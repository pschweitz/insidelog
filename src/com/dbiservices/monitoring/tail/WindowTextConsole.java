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
import com.dbiservices.monitoring.common.schedulerservice.IScheduledService;
import com.dbiservices.monitoring.common.schedulerservice.ScheduledDefinition;
import com.dbiservices.monitoring.common.schedulerservice.ServiceScheduler;
import com.dbiservices.monitoring.tail.textconsole.IOutputConsole;
import com.dbiservices.monitoring.tail.textconsole.SwingConsole;
import com.dbiservices.tools.Logger;
import java.io.File;
import java.util.concurrent.Executor;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class WindowTextConsole {

    private static final Logger logger = Logger.getLogger(WindowTextConsole.class);

    private static final int JLIST = 0;
    private static final int SWINGCONSOLE = 1;

    private IOutputConsole outputConsole;
    private Stage textConsoleStage;
    private int outputType = SWINGCONSOLE;

    private String title = "";

    private ToolBar toolbar;
    private Button start;
    private Button pause;
    private Button stop;
    private Button clean;
    private Button saveText;
    private Button saveRTF;
    private Button copyText;
    private Button copyRTF;
    private Button btColorChooser;
    private Button insertLine;
    private Button searchButton;

    private Button zoomIn;
    private Button zoomOut;
    private Button zoomReset;

    private Scene textConsoleScene;
    private InformationObject informationObject;
    private HBox statusBar;

    private boolean isRunning = true;
    private boolean taskStarted = false;

    private File lastSelected = null;

    private WindowTextConsoleSettings settings = new WindowTextConsoleSettings();

    BorderPane pane;

    public WindowTextConsole(String name, InformationObject informationObject) {
        this(informationObject);
        this.title = name;
        textConsoleStage.setTitle(title);
        textConsoleStage.getIcons().add(new Image("tail_small_logo.png"));
        updateButtons();
    }

    private WindowTextConsole(InformationObject informationObject) {
        this.informationObject = informationObject;
        createOrClearOutputConsole();

        toolbar = new ToolBar();
        statusBar = new HBox();

        start = new Button();
        start.setGraphic(new ImageView(new Image("control-green.png")));
        start.setTooltip(new Tooltip("Start"));
        start.setPrefWidth(30);
        start.setPrefHeight(30);
        start.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {
                        if (!ServiceScheduler.getScheduledDefinitionPool().containsKey(informationObject.getFullName())) {

                            IScheduledService iTail = null;

                            if (informationObject.getFilePath().toLowerCase().startsWith("ssh://")) {
                                iTail = new TailSSH(informationObject);
                            } else {
                                iTail = new TailFile(informationObject);
                            }

                            ServiceScheduler.addScheduledDefinition(informationObject.getFullName(), new ScheduledDefinition(informationObject, iTail));

                            isRunning = true;
                            updateButtons();
                        }
                    }
                }
        );

        pause = new Button();
        pause.setGraphic(new ImageView(new Image("control-pause.png")));
        pause.setTooltip(new Tooltip("Pause"));
        pause.setPrefWidth(30);
        pause.setPrefHeight(30);
        pause.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {
                        ServiceScheduler.removeScheduledDefinition(informationObject.getFullName());

                        isRunning = false;
                        updateButtons();
                    }
                }
        );

        stop = new Button();
        stop.setGraphic(new ImageView(new Image("control-stop-square.png")));
        stop.setTooltip(new Tooltip("Stop"));
        stop.setPrefWidth(30);
        stop.setPrefHeight(30);
        stop.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {

                        ServiceScheduler.removeScheduledDefinition(informationObject.getFullName());
                        informationObject.setLastFileLength(0);
                        informationObject.setOffset(0);
                        isRunning = false;
                        updateButtons();
                    }
                }
        );

        clean = new Button();
        clean.setGraphic(new ImageView(new Image("edit-clear.png")));
        clean.setTooltip(new Tooltip("Clean"));
        clean.setPrefWidth(30);
        clean.setPrefHeight(30);
        clean.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {

                        clear();

                    }
                }
        );

        saveText = new Button();
        saveText.setGraphic(new ImageView(new Image("disk-black.png")));
        saveText.setTooltip(new Tooltip("Save as Text"));
        saveText.setPrefWidth(30);
        saveText.setPrefHeight(30);

        saveText.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {

                        final FileChooser fileChooser = new FileChooser();

                        if (lastSelected != null) {
                            fileChooser.setInitialDirectory(lastSelected.getParentFile());
                        }
                        fileChooser.setInitialFileName(informationObject.getDisplayName());
                        fileChooser.getExtensionFilters().addAll(
                                new ExtensionFilter("Text Files", "*" + informationObject.getDisplayName().substring(informationObject.getDisplayName().lastIndexOf('.'))));

                        final File selectedFile = fileChooser.showSaveDialog(textConsoleStage);
                        if (selectedFile != null) {
                            lastSelected = selectedFile;

                            ProcessExecutor processExecutor = new ProcessExecutor("Save as Text");
                            processExecutor.execute(new ProcessWorkerSaveTEXT(selectedFile.getAbsolutePath()));

                        }
                    }
                }
        );

        saveRTF = new Button();
        saveRTF.setGraphic(new ImageView(new Image("diskRTF.png")));
        saveRTF.setTooltip(new Tooltip("Save as RTF"));
        saveRTF.setPrefWidth(30);
        saveRTF.setPrefHeight(30);

        saveRTF.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {

                        final FileChooser fileChooser = new FileChooser();

                        if (lastSelected != null) {
                            fileChooser.setInitialDirectory(lastSelected.getParentFile());
                        }
                        fileChooser.setInitialFileName(informationObject.getDisplayName() + ".rtf");

                        fileChooser.getExtensionFilters().addAll(
                                new ExtensionFilter("RTF Files", "*.rtf"));

                        final File selectedFile = fileChooser.showSaveDialog(textConsoleStage);
                        if (selectedFile != null) {
                            lastSelected = selectedFile;

                            ProcessExecutor processExecutor = new ProcessExecutor("Save as RTF");
                            processExecutor.execute(new ProcessWorkerSaveRTF(selectedFile.getAbsolutePath()));

                        }
                    }
                }
        );

        copyText = new Button();
        copyText.setGraphic(new ImageView(new Image("clipboard-paste-document-text.png")));
        copyText.setTooltip(new Tooltip("Copy to clipboard as row TEXT"));
        copyText.setPrefWidth(30);
        copyText.setPrefHeight(30);

        copyText.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {
                        ProcessExecutor processExecutor = new ProcessExecutor("Copy to clipboard");
                        processExecutor.execute(new ProcessWorkerCopyTEXT());
                    }
                }
        );

        copyRTF = new Button();
        copyRTF.setGraphic(new ImageView(new Image("clipboard-paste-document-text-color.png")));
        copyRTF.setTooltip(new Tooltip("Copy to clipboard as RTF"));
        copyRTF.setPrefWidth(30);
        copyRTF.setPrefHeight(30);

        copyRTF.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {
                        ProcessExecutor processExecutor = new ProcessExecutor("Copy to clipboard");
                        processExecutor.execute(new ProcessWorkerCopyRTF());
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
                        ColorChooserWindow colorChooserWindow = new ColorChooserWindow(informationObject, informationObject.getFileColors());
                        colorChooserWindow.start(new Stage());
                    }
                }
        );

        insertLine = new Button();
        insertLine.setGraphic(new ImageView(new Image("line.png")));
        insertLine.setTooltip(new Tooltip("Insert breaking line"));
        insertLine.setPrefWidth(30);
        insertLine.setPrefHeight(30);

        insertLine.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {
                        outputConsole.insertLine(informationObject.isDisplayColors());
                    }
                }
        );

        searchButton = new Button();
        searchButton.setGraphic(new ImageView(new Image("magnifier-zoom-actual-equal.png")));
        searchButton.setTooltip(new Tooltip("Search for text"));
        searchButton.setPrefWidth(30);
        searchButton.setPrefHeight(30);
        searchButton.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {
                        outputConsole.search();
                    }
                }
        );

        zoomIn = new Button();
        zoomIn.setGraphic(new ImageView(new Image("magnifier-zoom-in.png")));
        zoomIn.setTooltip(new Tooltip("Zoom In"));
        zoomIn.setPrefWidth(30);
        zoomIn.setPrefHeight(30);
        zoomIn.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {
                        outputConsole.zoomIn();
                    }
                }
        );

        zoomReset = new Button();
        zoomReset.setGraphic(new ImageView(new Image("magnifier-zoom-fitH.png")));
        zoomReset.setTooltip(new Tooltip("Zoom Reset"));
        zoomReset.setPrefWidth(30);
        zoomReset.setPrefHeight(30);
        zoomReset.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {
                        outputConsole.zoomReset();
                    }
                }
        );

        zoomOut = new Button();
        zoomOut.setGraphic(new ImageView(new Image("magnifier-zoom-out.png")));
        zoomOut.setTooltip(new Tooltip("Zoom Out"));
        zoomOut.setPrefWidth(30);
        zoomOut.setPrefHeight(30);
        zoomOut.setOnAction(
                new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent event) {
                        outputConsole.zoomOut();
                    }
                }
        );

        Separator separator = new Separator();
        separator.setOrientation(Orientation.VERTICAL);
        Separator separator2 = new Separator();
        separator2.setOrientation(Orientation.VERTICAL);
        Separator separator3 = new Separator();
        separator3.setOrientation(Orientation.VERTICAL);
        Separator separator4 = new Separator();
        separator4.setOrientation(Orientation.VERTICAL);
        Separator separator5 = new Separator();
        separator5.setOrientation(Orientation.VERTICAL);
        Separator separator6 = new Separator();
        separator6.setOrientation(Orientation.VERTICAL);
        Separator separator7 = new Separator();
        separator7.setOrientation(Orientation.VERTICAL);
        
        
        logger.warning("BUILD Toolbar");

        toolbar.getItems().add(start);
        toolbar.getItems().add(pause);
        toolbar.getItems().add(stop);
        toolbar.getItems().add(separator);
        toolbar.getItems().add(clean);
        toolbar.getItems().add(separator2);
        toolbar.getItems().add(saveText);
        toolbar.getItems().add(saveRTF);
        toolbar.getItems().add(separator3);
        toolbar.getItems().add(copyText);
        toolbar.getItems().add(copyRTF);
        toolbar.getItems().add(separator4);
        toolbar.getItems().add(btColorChooser);
        toolbar.getItems().add(separator5);
        toolbar.getItems().add(insertLine);
        toolbar.getItems().add(separator6);
        toolbar.getItems().add(settings.getSearchTextField());
        toolbar.getItems().add(settings.getMenuButton());
        toolbar.getItems().add(searchButton);
        toolbar.getItems().add(separator7);
        toolbar.getItems().add(zoomOut);
        toolbar.getItems().add(zoomReset);
        toolbar.getItems().add(zoomIn);

        toolbar.getItems().add(new HBox());
        pane = new BorderPane();
        pane.setTop(toolbar);
        pane.setCenter((Parent) getConsole());
        textConsoleScene = new Scene(pane);

        textConsoleStage = new Stage();
        textConsoleStage.setHeight(500);
        textConsoleStage.setWidth(850);
        textConsoleScene.setFill(Color.web("#dce6ec"));

        textConsoleStage.setScene(textConsoleScene);

        textConsoleStage.setOnCloseRequest(new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {

                ServiceScheduler.removeScheduledDefinition(informationObject.getFullName());
                informationObject.setLastFileLength(0);
                informationObject.setOffset(0);
                isRunning = false;
                updateButtons();

                if (!DbiTail.showMainStage) {
                    System.exit(0);
                }
            }
        }
        );
    }

    private void createOrClearOutputConsole() {
        if (outputConsole != null) {
            outputConsole.clear();
            double height = ((Pane) getConsole()).getHeight();
            double width = ((Pane) getConsole()).getWidth();

            ((Pane) getConsole()).setPrefSize(width, height);
        }

        switch (outputType) {

            case JLIST:

                break;

            case SWINGCONSOLE:

                if (informationObject != null) {
                    outputConsole = new SwingConsole(informationObject);
                    ((SwingConsole) outputConsole).setSettings(settings);
                }
                break;
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
        updateButtons();
    }

    private void updateButtons() {

        if (isRunning) {

            start.setDisable(true);
            saveText.setDisable(true);
            saveRTF.setDisable(true);
            copyText.setDisable(true);
            copyRTF.setDisable(true);

            pause.setDisable(false);
            stop.setDisable(false);

        } else {

            start.setDisable(false);
            saveText.setDisable(false);
            saveRTF.setDisable(false);
            copyText.setDisable(false);
            copyRTF.setDisable(false);

            pause.setDisable(true);
            stop.setDisable(true);
        }
    }

    public void setName(String name) {
        textConsoleStage.setTitle(name);
        this.title = name;
    }

    public void show() {
        textConsoleStage.show();
        textConsoleStage.toFront();
    }

    public void hide() {
        textConsoleStage.hide();
    }

    public void readSavedFile() {
        logger.info("Open of file: " + informationObject.getFullName());

        informationObject.setOffset(-1);
        TailFile tail = new TailFile(informationObject);

        if (!ServiceScheduler.getScheduledDefinitionPool().containsKey(informationObject.getFullName())) {
            ServiceScheduler.addScheduledDefinition(informationObject.getFullName(), new ScheduledDefinition(informationObject, tail));
        }
    }

    public void appendText(final String content) {

        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                if (getConsole() != null) {
                    outputConsole.appendText(content, informationObject);
                }
            }
        });
    }

    public void insertLine(boolean displayColors) {

        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                if (getConsole() != null) {
                    outputConsole.insertLine(displayColors);
                }
            }
        });
    }

    private IOutputConsole getConsole() {
        return outputConsole;
    }

    public void clear() {
        createOrClearOutputConsole();

        pane.setCenter((Parent) getConsole());

        textConsoleScene.setRoot(new BorderPane());
        textConsoleScene = new Scene(pane);
        textConsoleStage.setScene(textConsoleScene);
    }

    private class ProcessExecutor implements Executor {

        String message = "";

        public ProcessExecutor(String message) {
            this.message = message;
        }

        @Override
        public void execute(Runnable command) {

            statusBar = new HBox();
            statusBar.setAlignment(Pos.BOTTOM_RIGHT);

            if (!taskStarted) {

                Label messageLabel = new Label(message);

                messageLabel.setTextFill(Color.web("#6880e6"));

                ProgressBar progressBar = new ProgressBar(-1);
                statusBar.getChildren().add(messageLabel);
                statusBar.getChildren().add(new Label(" "));
                statusBar.getChildren().add(progressBar);
                pane.setBottom(statusBar);

                new Thread(command).start();
                taskStarted = true;
            } else {

                Label warning = new Label("An action is already runnung ->");
                Label messageLabel = new Label(message);

                warning.setTextFill(Color.web("#ff6666"));
                messageLabel.setTextFill(Color.web("#6880e6"));

                statusBar.getChildren().add(warning);
                ProgressBar progressBar = new ProgressBar(-1);
                statusBar.getChildren().add(messageLabel);
                statusBar.getChildren().add(new Label(" "));
                statusBar.getChildren().add(progressBar);
                pane.setBottom(statusBar);
            }
        }
    }

    private class ProcessWorkerCopyRTF implements Runnable {

        @Override
        public void run() {
            outputConsole.copyStyledContent();
            taskStarted = false;

            Label message = new Label("Done !");

            message.setTextFill(Color.web("#6880e6"));

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    statusBar = new HBox();
                    statusBar.setAlignment(Pos.BOTTOM_RIGHT);
                    statusBar.getChildren().add(message);
                    statusBar.getChildren().add(new Label(" "));
                    pane.setBottom(statusBar);
                }
            });
        }
    }

    private class ProcessWorkerSaveRTF implements Runnable {

        private String destinationFile = "";

        public ProcessWorkerSaveRTF(String destinationFile) {
            this.destinationFile = destinationFile;
        }

        @Override
        public void run() {
            outputConsole.saveStyledContent(destinationFile);
            taskStarted = false;

            Label message = new Label("Done !");

            message.setTextFill(Color.web("#6880e6"));

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    statusBar = new HBox();
                    statusBar.setAlignment(Pos.BOTTOM_RIGHT);
                    statusBar.getChildren().add(message);
                    statusBar.getChildren().add(new Label(" "));
                    pane.setBottom(statusBar);
                }
            });
        }
    }

    private class ProcessWorkerCopyTEXT implements Runnable {

        @Override
        public void run() {
            outputConsole.copyTextContent();
            taskStarted = false;

            Label message = new Label("Done !");

            message.setTextFill(Color.web("#6880e6"));

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    statusBar = new HBox();
                    statusBar.setAlignment(Pos.BOTTOM_RIGHT);
                    statusBar.getChildren().add(message);
                    statusBar.getChildren().add(new Label(" "));
                    pane.setBottom(statusBar);
                }
            });
        }
    }

    private class ProcessWorkerSaveTEXT implements Runnable {

        private String destinationFile = "";

        public ProcessWorkerSaveTEXT(String destinationFile) {
            this.destinationFile = destinationFile;
        }

        @Override
        public void run() {
            outputConsole.saveTextContent(destinationFile);
            taskStarted = false;

            Label message = new Label("Done !");

            message.setTextFill(Color.web("#6880e6"));

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    statusBar = new HBox();
                    statusBar.setAlignment(Pos.BOTTOM_RIGHT);
                    statusBar.getChildren().add(message);
                    statusBar.getChildren().add(new Label(" "));
                    pane.setBottom(statusBar);
                }
            });
        }
    }

    public void setInformationObject(InformationObject informationObject) {
        this.outputConsole.setInformationObject(informationObject);
    }
}
