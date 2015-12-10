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
import com.dbiservices.tools.Logger;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

public class TailSSH implements IScheduledService, Serializable {

    private static final Logger logger = Logger.getLogger(TailSSH.class);

    private BlockingQueue<String> messageQueue;

    public BufferedInputStream consoleInput = null;
    public BufferedOutputStream console = null;

    public BufferedInputStream commandLineInput = null;
    public BufferedOutputStream commandLine = null;

    private InformationObject informationObject;

    private boolean running = false;

    private Channel channel;
    private Session session;
    private OutputStreamWriter prompt;

    private static final Hashtable<String, Hashtable> hostsInformation = new Hashtable();

    public TailSSH(InformationObject informationObject) {
        this.informationObject = informationObject;
        this.running = true;
    }

    private static String getPassword(String user, String host) {
        String result = "";

        if (hostsInformation.containsKey(host)) {
            if (hostsInformation.get(host).containsKey(user)) {
                result = (String) hostsInformation.get(host).get(user);
            }
        }

        return result;
    }

    private static void updatePassword(String user, String host, String passwd) {

        if (!user.equals("") && !host.equals("")) {
            if (!hostsInformation.containsKey(host)) {
                hostsInformation.put(host, new Hashtable());
            }

            if (!hostsInformation.get(host).containsKey(user)) {
                hostsInformation.get(host).put(user, passwd);
            } else {
                hostsInformation.get(host).replace(user, passwd);
            }
        }
    }

    private static void removePassword(String user, String host, String passwd) {

        if (!user.equals("") && !host.equals("")) {
            if (hostsInformation.containsKey(host)) {
                if (hostsInformation.get(host).containsKey(user)) {
                    hostsInformation.get(host).remove(user);
                }
            }
        }
    }

    public boolean connect() {

        messageQueue = new LinkedBlockingQueue();

        Charset charset = informationObject.getCharset();

        boolean openSession = true;
        String filename = informationObject.getFilePath().substring("ssh://".length());
        String host = "";
        String user = "";
        String passwd = "";

        try {

            PipedOutputStream poCommand = new PipedOutputStream();
            PipedInputStream piCommand = new PipedInputStream(poCommand);
            commandLineInput = new BufferedInputStream(piCommand);
            commandLine = new BufferedOutputStream(poCommand);

            PipedOutputStream poMessage = new PipedOutputStream();
            PipedInputStream piMessage = new PipedInputStream(poMessage);
            consoleInput = new BufferedInputStream(piMessage);
            console = new BufferedOutputStream(poMessage);

            JSch jsch = new JSch();

            logger.debug("filename: " + filename);

            if (filename.contains("@")) {
                user = filename.substring(0, filename.indexOf('@'));
                filename = filename.substring(filename.indexOf('@') + 1);
            }

            if (!filename.contains(":")) {
                openSession = false;
            } else {
                host = filename.substring(0, filename.indexOf(':'));
                filename = filename.substring(filename.indexOf(':') + 1);
            }

            if (!user.equals("") && !host.equals("")) {
                passwd = getPassword(user, host);
            }

            if (passwd.equals("")) {
                Optional<Pair<String, String>> resultPair = getCredentials(user, host);

                if (resultPair != null) {
                    user = resultPair.get().getKey();
                    passwd = resultPair.get().getValue();
                }
            }

            logger.debug("host: " + host);
            logger.debug("user: " + user);
            logger.debug("password: ****** ");
            logger.debug("filename: " + filename);
            logger.debug("openSession: " + openSession);

            if (user.equals("")) {
                openSession = false;
            }

            if (openSession) {
                session = jsch.getSession(user, host, 22);

                session.setPassword(passwd);

                UserInfo ui = new MyUserInfo() {
                    public void showMessage(String message) {
                    }

                    public boolean promptYesNo(String message) {
                        return true;
                    }
                };
                session.setUserInfo(ui);

                session.connect(30000);   // making a connection with timeout.

                updatePassword(user, host, passwd);

                channel = session.openChannel("shell");

                channel.setInputStream(commandLineInput);

                CharsetDetect charsetDetect = null;

                InputStreamReader is = null;

                if (charset == null) {

                    /*
                     PipedOutputStream poMessageDetect = new PipedOutputStream();
                     PipedInputStream piMessageDetect = new PipedInputStream(poMessageDetect);
                     BufferedInputStream consoleInputDetect = new BufferedInputStream(piMessageDetect);
                     BufferedOutputStream consoleDetect = new BufferedOutputStream(poMessageDetect);

                     charsetDetect = new CharsetDetect(consoleInput, consoleDetect, informationObject, Long.MAX_VALUE); // 1Mo of check
                     charsetDetect.start();
                     charset = charsetDetect.charset;
                    
                     is = new InputStreamReader(consoleInputDetect, charset);
                     */
                    charset = Charset.forName("US-ASCII");

                }
                //else {

                is = new InputStreamReader(consoleInput, charset);
                //}

                BufferedReader br = new BufferedReader(is);

                ConsoleStreamReader streamReader = new ConsoleStreamReader(br);
                streamReader.start();

                channel.setOutputStream(console);

                channel.connect(3 * 1000);

                prompt = new OutputStreamWriter(commandLine);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                }

                prompt.append("tail -f " + filename).append(System.lineSeparator());
                prompt.flush();

            }
        } catch (IOException | JSchException e) {

            removePassword(user, host, passwd);

            if (e.toString().equals("com.jcraft.jsch.JSchException: Auth cancel")) {
                messageQueue.add(" ");
                messageQueue.add("error: Username or password incorrect");
                messageQueue.add(" ");
            } else {
                logger.error("Error connecting to host: " + host, e);
            }
        }

        return openSession;
    }

    private class ConsoleStreamReader extends Thread {

        private BufferedReader reader = null;

        public ConsoleStreamReader(BufferedReader reader) {
            super("ConsoleStreamReader");

            this.reader = reader;
        }

        @Override
        public void run() {

            while (running) {

                try {
                    String input = "";

                    input = this.reader.readLine();

                    while (!input.equals("")) {
                        messageQueue.add(input);
                        input = this.reader.readLine();
                    }

                } catch (Exception e) {

                }
            }

            try {
                prompt.close();
                session.disconnect();
                channel.disconnect();
                this.reader.close();
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void ScheduledAction() {

        parseFile();
    }

    public void parseFile() {

        int bufferSize = informationObject.getBufferSize();

        StringBuilder stringBuider = new StringBuilder();

        try {

            String line = "";
            int counter = 0;

            while (running) {

                line = messageQueue.take();

                if (!line.equals("") && !line.equals("\r")) {

                    if (line.charAt(line.length() - 1) != '\n' || !line.substring(line.length() - 2).equals("\r\n")) {
                        stringBuider.append(line).append(System.lineSeparator());

                    } else {
                        stringBuider.append(line);
                    }
                }
                counter++;

                if (counter == bufferSize || messageQueue.isEmpty()) {
                    informationObject.setElementCount(counter);
                    informationObject.getWindowTextConsole().appendText(stringBuider.toString());
                    stringBuider = new StringBuilder();
                    counter = 0;
                }
            }

        } catch (InterruptedException e) {
            logger.error("Error opening file", e);
            informationObject.getWindowTextConsole().insertLine(true);
            informationObject.getWindowTextConsole().insertLine(true);
            informationObject.getWindowTextConsole().appendText("Error opening file: " + e.toString());
        }

        try {
            prompt.close();
            session.disconnect();
            channel.disconnect();
        } catch (Exception ex) {
        }
    }

    public InformationObject getInformationObject() {
        return informationObject;
    }

    public void setInformationObject(InformationObject informationObject) {
        this.informationObject = informationObject;
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        logger.debug("SSH: setEnabled " + isEnabled);

        running = isEnabled;

        if (!isEnabled) {
            try {
                prompt.append("exit").append(System.lineSeparator());
                prompt.flush();
            } catch (IOException ex) {
            }
        }
    }

    public static abstract class MyUserInfo implements UserInfo, UIKeyboardInteractive {

        public String getPassword() {
            return null;
        }

        public boolean promptYesNo(String str) {
            return false;
        }

        public String getPassphrase() {
            return null;
        }

        public boolean promptPassphrase(String message) {
            return false;
        }

        public boolean promptPassword(String message) {
            return false;
        }

        public void showMessage(String message) {
        }

        public String[] promptKeyboardInteractive(String destination,
                String name,
                String instruction,
                String[] prompt,
                boolean[] echo) {
            return null;
        }
    }

    private Optional<Pair<String, String>> getCredentials(String usernameText, String hostname) {

        // Create the custom dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Login Dialog");
        dialog.setHeaderText("Connection to host " + hostname);

// Set the icon (must be included in the project).
        dialog.setGraphic(new ImageView(new Image("login.png")));

// Set the button types.
        ButtonType loginButtonType = new ButtonType("Login", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

// Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        username.setText(usernameText);
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

// Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

// Do some validation (using the Java 8 lambda syntax).
        password.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(false);
        });

        dialog.getDialogPane().setContent(grid);

        if (usernameText.equals("")) {
            Platform.runLater(() -> username.requestFocus());
        } else {
            Platform.runLater(() -> password.requestFocus());
        }

// Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        return result;
        /*
         result.ifPresent(usernamePassword -> {
         System.out.println("Username=" + usernamePassword.getKey() + ", Password=" + usernamePassword.getValue());
         });*/
    }
}
