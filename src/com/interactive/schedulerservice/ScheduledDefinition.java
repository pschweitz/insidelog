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
package com.interactive.schedulerservice;

/**
 *
 * @author Philippe Schweitzer
 * @version 1.1
 * @since 16.11.2015
 */
import com.interactive.insidelog.InformationObject;
import com.interactive.tools.Logger;
import java.util.Timer;
import java.util.TimerTask;

public class ScheduledDefinition {

    private static final Logger logger = Logger.getLogger(ScheduledDefinition.class);

    private String fullName;
    private String displayName;
    private int executionType = ServiceScheduler.NONE;
    private long waitMS = 1000;
    private IScheduledService iScheduledService;
    private Timer timer;
    private Task task = new Task();
    private boolean isEnabled;
    private InformationObject informationObject;

    public ScheduledDefinition(InformationObject informationObject, IScheduledService iScheduledService) {
        this.fullName = informationObject.getFullName();
        this.displayName = informationObject.getDisplayName();
        this.iScheduledService = iScheduledService;
        this.executionType = informationObject.getScheduleType();
        this.waitMS = informationObject.getFrequency();
        this.timer = new Timer();
        this.informationObject = informationObject;
        this.setEnabled(true);

        if (executionType == ServiceScheduler.REGULAR) {
            timer.scheduleAtFixedRate(task, waitMS, waitMS);
        } else if (executionType == ServiceScheduler.MATURITY) {
            timer.schedule(task, waitMS);
        }
    }

    public IScheduledService getiScheduledService() {
        return iScheduledService;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;

        iScheduledService.setEnabled(isEnabled);
    }

    public InformationObject getInformationObject() {
        return informationObject;
    }

    public String getDisplayName() {
        return displayName;
    }

    private class Task extends TimerTask {

        @Override
        public void run() {
            if (isEnabled()) {
                iScheduledService.ScheduledAction();
            } else if (!isEnabled() || executionType == ServiceScheduler.MATURITY) {
                timer.cancel();
            }
        }
    }
}
