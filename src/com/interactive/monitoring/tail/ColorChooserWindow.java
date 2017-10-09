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

package com.interactive.monitoring.tail;

/**
 *
 * @author  Philippe Schweitzer
 * @version 1.1
 * @since   16.11.2015
 */

import com.interactive.tools.Logger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ColorChooserWindow extends Application {

    private static final Logger logger = Logger.getLogger(ColorChooserWindow.class);

    private final InformationObject informationObject;

    private String fileColors = Insidelog.colorFileName;

    public ColorChooserWindow(InformationObject informationObject, String fileColors) {
        this.informationObject = informationObject;
        this.fileColors = fileColors;
    }

    @Override
    public void start(Stage primaryStage) {

        try {

            ColorChooserWindowControler colorChooserWindowControler = new ColorChooserWindowControler(this.informationObject, fileColors, primaryStage);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "ColorChooserScene.fxml"
                    )
            );
            loader.setController(colorChooserWindowControler);

            Parent root = loader.load();

            Scene scene = new Scene(root, 663, 365);

            primaryStage.setTitle("Color chooser - " + informationObject.getFullName());
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();

        } catch (Exception e) {
            logger.error("Error in loading primaryStage for ColorChooserWindow: " + e.toString());
            e.printStackTrace();
        }
    }
}
