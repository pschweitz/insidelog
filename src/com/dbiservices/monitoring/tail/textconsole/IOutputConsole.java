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

package com.dbiservices.monitoring.tail.textconsole;

/**
 *
 * @author  Philippe Schweitzer
 * @version 1.1
 * @since   16.11.2015
 */

import com.dbiservices.monitoring.tail.InformationObject;

public interface IOutputConsole {
    
    public void appendText(String content, InformationObject informationObject);
    public void insertLine(boolean displayColors);
    public void clear();    
    public void saveStyledContent(String destinationFile); 
    public void saveTextContent(String destinationFile); 
    public void copyStyledContent(); 
    public void copyTextContent();    
    public void search(String text, boolean caseSensitive, boolean wholeWord);
    public void zoomIn();    
    public void zoomOut();   
    public void zoomReset();       
    public void setInformationObject(InformationObject informationObject);
}
