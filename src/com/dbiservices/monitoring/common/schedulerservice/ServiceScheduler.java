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

package com.dbiservices.monitoring.common.schedulerservice;

/**
 *
 * @author  Philippe Schweitzer
 * @version 1.1
 * @since   16.11.2015
 */

import com.dbiservices.tools.Logger;
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
        scheduledDefinition.refreshWindowConfiguration();
        logger.info("Start read of file: " + scheduledDefinition.getDisplayName());
        scheduledDefinitionPool.put(fullName, scheduledDefinition);
    }

    public static void removeScheduledDefinition(String fullName) {

        logger.trace("removeScheduledDefinition: " + fullName);
        if (scheduledDefinitionPool.containsKey(fullName)) {
            logger.info("Stop read of file: " + scheduledDefinitionPool.get(fullName).getDisplayName());
            scheduledDefinitionPool.get(fullName).setEnabled(false);
            scheduledDefinitionPool.remove(fullName);
        }
    }

    public ScheduledDefinition getScheduledDefinition(String fullName) {
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

    public static void setScheduledDefinitionPool(Hashtable<String, ScheduledDefinition> scheduledDefinitionPool) {
        scheduledDefinitionPool = scheduledDefinitionPool;
    }
}
