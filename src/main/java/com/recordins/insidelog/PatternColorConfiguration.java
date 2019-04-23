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

import com.recordins.tools.Logger;
import javafx.scene.paint.Color;

public class PatternColorConfiguration {

    private static final Logger logger = Logger.getLogger(PatternColorConfiguration.class);

    public String pattern;
    public Color color;
    public Boolean caseSentitive;

    public PatternColorConfiguration(String pattern, Color color, Boolean caseSentitive) {
        this.pattern = pattern;
        this.color = color;
        this.caseSentitive = caseSentitive;
    }

    public boolean isCaseSentitive() {
        return caseSentitive;
    }

    public void setCaseSentitive(boolean caseSentitive) {
        this.caseSentitive = caseSentitive;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Color getColor() {
        return color;
    }

    public java.awt.Color getSwingColor() {
        return ColorConfiguration.getSwingColor(color);
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
