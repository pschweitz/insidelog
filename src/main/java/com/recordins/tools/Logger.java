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

package com.recordins.tools;

/**
 * @author Philippe Schweitzer
 * @version 1.1
 * @since 16.11.2015
 */

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Logger extends Thread {

    private static final BlockingQueue<String> messageQueue = new LinkedBlockingQueue();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d yyyy z HH:mm:ss.SSS");
    private static final Logger reader = new Logger();

    private static boolean listening = true;
    private static LogLevel logLevel;

    private String className;

    public enum LogLevel {

        TRACE(0, "Trace"),
        DEBUG(1, "Debug"),
        INFO(2, "Info"),
        WARNING(3, "Warning"),
        ERROR(4, "Error"),
        CRITICAL(5, "Critical");

        private int level;
        private String levelDescription;

        public String getDescription() {
            return levelDescription;
        }

        private int getLevel() {
            return level;
        }

        public static LogLevel findLevel(String description) {
            LogLevel result = LogLevel.INFO;
            boolean descriptionFound = false;

            description = description.toLowerCase();

            for (LogLevel loglevel : values()) {
                if (loglevel.levelDescription.toLowerCase().equals(description)) {
                    result = loglevel;
                    descriptionFound = true;
                    break;
                }
            }

            if (!descriptionFound) {
                try {
                    messageQueue.put(dateFormat.format(new Date()) + " [" + Thread.currentThread().getName() + "] WARNING " + Logger.class.getName() + " - LogLevel description not found: \"" + description + "\", using default level: \"" + result.getDescription() + "\"");
                } catch (InterruptedException ex) {
                }
            }

            return result;
        }

        public boolean isLevel(LogLevel logLevel) {
            return level <= logLevel.getLevel();
        }

        LogLevel(int level, String levelDescription) {
            this.level = level;
            this.levelDescription = levelDescription;
        }
    }

    private Logger() {
        this.className = Logger.class.getName();

        Logger.logLevel = LogLevel.INFO;

        this.start();
    }

    private Logger(Class classObject) {
        this.className = classObject.getName();
    }

    @Override
    public void run() {
        while (listening) {
            try {
                System.out.println(this.messageQueue.take());
            } catch (InterruptedException ex) {
            }
        }
    }

    public static Logger getLogger(Class className) {
        return new Logger(className);
    }

    public static boolean isListening() {
        return listening;
    }

    public static void setListening(boolean listening) {
        Logger.listening = listening;
    }

    public static LogLevel getLogLevel() {
        return logLevel;
    }

    public static void setLogLevel(LogLevel logLevel) {
        Logger.logLevel = logLevel;

        try {
            messageQueue.put(dateFormat.format(new Date()) + " [" + Thread.currentThread().getName() + "] INFO " + Logger.class.getName() + " - " + "LogLevel: " + Logger.logLevel.getDescription());
        } catch (InterruptedException ex) {
        }
    }

    public void trace(String message) {

        if (Logger.logLevel.isLevel(LogLevel.TRACE)) {
            try {
                messageQueue.put(dateFormat.format(new Date()) + " [" + Thread.currentThread().getName() + "] TRACE " + this.className + " - " + message);
            } catch (InterruptedException ex) {
            }
        }
    }

    public void debug(String message) {

        if (Logger.logLevel.isLevel(LogLevel.DEBUG)) {
            try {
                messageQueue.put(dateFormat.format(new Date()) + " [" + Thread.currentThread().getName() + "] DEBUG " + this.className + " - " + message);
            } catch (InterruptedException ex) {
            }
        }
    }

    public void info(String message) {

        if (Logger.logLevel.isLevel(LogLevel.INFO)) {
            try {
                messageQueue.put(dateFormat.format(new Date()) + " [" + Thread.currentThread().getName() + "] INFO " + this.className + " - " + message);
            } catch (InterruptedException ex) {
            }
        }
    }

    public void warning(String message) {

        if (Logger.logLevel.isLevel(LogLevel.WARNING)) {
            try {
                messageQueue.put(dateFormat.format(new Date()) + " [" + Thread.currentThread().getName() + "] WARNING " + this.className + " - " + message);
            } catch (InterruptedException ex) {
            }
        }
    }

    public void warning(String message, Exception e) {
        StringBuilder output = new StringBuilder();

        if (Logger.logLevel.isLevel(LogLevel.WARNING)) {
            try {
                output.append(dateFormat.format(new Date()) + " [" + Thread.currentThread().getName() + "] WARNING " + this.className + " - " + message + ": " + e.toString());
                output.append(System.lineSeparator());

                for (StackTraceElement element : e.getStackTrace()) {
                    output.append(element.toString());
                    output.append(System.lineSeparator());
                }

                messageQueue.put(output.toString());

            } catch (InterruptedException ex) {
            }
        }
    }

    public void error(String message) {

        if (Logger.logLevel.isLevel(LogLevel.ERROR)) {
            try {
                messageQueue.put(dateFormat.format(new Date()) + " [" + Thread.currentThread().getName() + "] ERROR " + this.className + " - " + message);
            } catch (InterruptedException ex) {
            }
        }
    }

    public void error(String message, Exception e) {
        StringBuilder output = new StringBuilder();

        if (Logger.logLevel.isLevel(LogLevel.ERROR)) {
            try {
                output.append(dateFormat.format(new Date()) + " [" + Thread.currentThread().getName() + "] ERROR " + this.className + " - " + message + ": " + e.toString());
                output.append(System.lineSeparator());

                for (StackTraceElement element : e.getStackTrace()) {
                    output.append(element.toString());
                    output.append(System.lineSeparator());
                }

                messageQueue.put(output.toString());

            } catch (InterruptedException ex) {
            }
        }
    }

    public void critical(String message) {

        if (Logger.logLevel.isLevel(LogLevel.CRITICAL)) {
            try {
                messageQueue.put(dateFormat.format(new Date()) + " [" + Thread.currentThread().getName() + "] CRITICAL " + this.className + " - " + message);
            } catch (InterruptedException ex) {
            }
        }
    }

    public void critical(String message, Exception e) {
        StringBuilder output = new StringBuilder();

        if (Logger.logLevel.isLevel(LogLevel.CRITICAL)) {
            try {
                output.append(dateFormat.format(new Date()) + " [" + Thread.currentThread().getName() + "] CRITICAL " + this.className + " - " + message + ": " + e.toString());
                output.append(System.lineSeparator());

                for (StackTraceElement element : e.getStackTrace()) {
                    output.append(element.toString());
                    output.append(System.lineSeparator());
                }

                messageQueue.put(output.toString());

            } catch (InterruptedException ex) {
            }
        }
    }
}
