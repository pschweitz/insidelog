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
package com.recordins.schedulerservice;

/**
 * @author Philippe Schweitzer
 * @version 1.1
 * @since 16.11.2015
 */

import com.recordins.insidelog.TailSSH;
import com.recordins.tools.Logger;

import java.util.Hashtable;

public class ServiceScheduler {

    private static final Logger logger = Logger.getLogger(ServiceScheduler.class);

    private static Hashtable<String, ScheduledDefinition> scheduledDefinitionPool = new Hashtable();

    public static final int NONE = 0;
    public static final int MATURITY = 1;
    public static final int REGULAR = 2;

    public ServiceScheduler() {
    }

    public static void addScheduledDefinition(String fullName, ScheduledDefinition scheduledDefinition) {
        logger.trace("addScheduledDefinition: " + fullName);
        scheduledDefinition.setEnabled(true);
        boolean success = true;

        if (TailSSH.class.isAssignableFrom(scheduledDefinition.getiScheduledService().getClass())) {
            success = ((TailSSH) scheduledDefinition.getiScheduledService()).connect();

            String OS = System.getProperty("os.name").toLowerCase();

            if (scheduledDefinition.getInformationObject().getWindowTextConsole() != null && (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0)) {
                scheduledDefinition.getInformationObject().getWindowTextConsole().hide();
                scheduledDefinition.getInformationObject().getWindowTextConsole().show();
            }
        }

        if (success) {
            logger.info("Start read of file: " + scheduledDefinition.getDisplayName());
            scheduledDefinitionPool.put(fullName, scheduledDefinition);
        }
    }

    public static void removeScheduledDefinition(String fullName) {

        logger.trace("removeScheduledDefinition: " + fullName);
        if (scheduledDefinitionPool.containsKey(fullName)) {
            logger.info("Stop read of file: " + scheduledDefinitionPool.get(fullName).getDisplayName());

            if (scheduledDefinitionPool.get(fullName).getiScheduledService().getInformationObject().getWindowTextConsole() != null) {
                scheduledDefinitionPool.get(fullName).getiScheduledService().getInformationObject().getWindowTextConsole().setIsRunning(false);
            }

            scheduledDefinitionPool.get(fullName).setEnabled(false);
            scheduledDefinitionPool.remove(fullName);
        }
    }

    public static ScheduledDefinition getScheduledDefinition(String fullName) {
        if (scheduledDefinitionPool.containsKey(fullName)) {
            return scheduledDefinitionPool.get(fullName);
        } else {
            return null;
        }
    }

    public static void enableScheduledDefinition(String fullName) {
        if (scheduledDefinitionPool.containsKey(fullName)) {
            scheduledDefinitionPool.get(fullName).setEnabled(true);
        }
    }

    public static void disableScheduledDefinition(String fullName) {
        if (scheduledDefinitionPool.containsKey(fullName)) {
            scheduledDefinitionPool.get(fullName).setEnabled(false);
        }
    }

    public static Hashtable<String, ScheduledDefinition> getScheduledDefinitionPool() {
        return scheduledDefinitionPool;
    }
}
