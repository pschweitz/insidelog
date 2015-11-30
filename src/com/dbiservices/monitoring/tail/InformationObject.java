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
 * @author  Philippe Schweitzer
 * @version 1.1
 * @since   16.11.2015
 */

import com.dbiservices.monitoring.common.schedulerservice.ServiceScheduler;
import com.dbiservices.tools.ApplicationContext;
import com.dbiservices.tools.Logger;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringTokenizer;

public class InformationObject implements Serializable {

    private static final Logger logger = Logger.getLogger(InformationObject.class);
    
    private String displayName;
    private String fullName;
    private Path filePath;
    private int frequency;
    private int scheduleType;
    private boolean isEnabled;
    private long offset;
    private int bufferSize;
    private boolean displayColors = false;
    private Charset charset = null;
    private long lastFileLength = 0;
    private int elementCount = 0;
    private String fileColors = "etc/color.cfg";
    private ColorConfiguration colorConfiguration = null;
    
    public InformationObject(String displayName, String fullName, Path filePath, int bufferSize, int frequency, boolean displayColors, String fileColors) {
        this(fullName, filePath, bufferSize, frequency, displayColors);
        this.displayName = displayName;
        this.fileColors = fileColors;
        this.colorConfiguration = new ColorConfiguration(fileColors);
    }

    private InformationObject(String fullName, Path filePath, int bufferSize, int frequency, boolean displayColors) {

        this.displayName = fullName;
        this.fullName = fullName;
        this.filePath = filePath;
        this.bufferSize = bufferSize;
        this.frequency = frequency;
        this.isEnabled = false;
        this.scheduleType = ServiceScheduler.REGULAR;
        this.offset = 0;
        this.displayColors = displayColors;
        this.colorConfiguration = (ColorConfiguration) ApplicationContext.getInstance().get("colorDefaultConfiguration");
    }

    public boolean isDisplayColors() {
        return displayColors;
    }

    public void setDisplayColors(boolean displayColors) {
        this.displayColors = displayColors;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    private void setName(String name) {
        this.displayName = name;
    }

    public void setFullName(String fullName) {

        WindowTextConsole windowTextConsole = (WindowTextConsole) ApplicationContext.getInstance().get(this.fullName);
        if (windowTextConsole != null) {
            windowTextConsole.setName(fullName);
            ApplicationContext.getInstance().put(fullName, windowTextConsole);
            ApplicationContext.getInstance().remove(this.fullName);            
        }

        this.fullName = fullName;
        String name = fullName;
        StringTokenizer stringTokenizer = new StringTokenizer(fullName, "/");

        while (stringTokenizer.hasMoreElements()) {
            name = stringTokenizer.nextToken();
        }

        setName(name);
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFullName() {
        return fullName;
    }

    public Path getFilePath() {
        Path result = this.filePath;

        if (result == null) {
            result = Paths.get(".");
        }
        return result;
    }

    public int getFrequency() {
        return frequency;
    }

    public long getOffset() {
        return offset;
    }

    public int getScheduleType() {
        return scheduleType;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public WindowTextConsole getWindowTextConsole() {
        return (WindowTextConsole) ApplicationContext.getInstance().get(this.fullName);
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public long getLastFileLength() {
        return lastFileLength;
    }

    public void setLastFileLength(long lastFileSize) {
        this.lastFileLength = lastFileSize;
    }

    public int getElementCount() {
        return elementCount;
    }

    public void setElementCount(int elementCount) {
        this.elementCount = elementCount;
    }

    public String getFileColors() {
        return fileColors;
    }

    public void setFileColors(String fileColors) {
        this.fileColors = fileColors;
    }

    public ColorConfiguration getColorConfiguration() {
        return colorConfiguration;
    }

    public void setColorConfiguration(ColorConfiguration colorConfiguration) {
        this.colorConfiguration = colorConfiguration;
    }

}
