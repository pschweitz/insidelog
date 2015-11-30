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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.SortedMap;
import org.mozilla.universalchardet.UniversalDetector;

public class Tail implements IScheduledService, Serializable {

    private static final Logger logger = Logger.getLogger(Tail.class);

    private InformationObject informationObject;

    private boolean running = false;

    public Tail(InformationObject informationObject) {
        this.informationObject = informationObject;
    }

    public void ScheduledAction() {

        parseFile();
    }

    private Charset detectCharset(File file) {

        Charset result = StandardCharsets.US_ASCII;
        byte[] buf = new byte[4096];

        SortedMap<String, Charset> charsetMap = Charset.availableCharsets();

        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream(file);

            UniversalDetector detector = new UniversalDetector(null);

            int nread;
            while ((nread = fstream.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }

            detector.dataEnd();

            String encoding = detector.getDetectedCharset();
            if (encoding != null) {
                logger.info("Detected encoding = " + encoding);

                if (charsetMap.containsKey(encoding.toUpperCase())) {

                    logger.info("Detected encoding in supported list !");
                    result = charsetMap.get(encoding.toUpperCase());

                }
            } else {
                logger.warning("No encoding detected. Using default: " + result.name());
            }

            detector.reset();
            fstream.close();

        } catch (Exception e) {
        } finally {
            try {
                fstream.close();
            } catch (Exception ex) {
            }
        }

        return result;
    }

    public void parseFile() {
        File file = new File(informationObject.getFilePath().toString());

        BufferedReader br = null;
        long offset = informationObject.getOffset();
        int bufferSize = informationObject.getBufferSize();
        Charset charset = informationObject.getCharset();
        long lastFileLength = informationObject.getLastFileLength();

        if (bufferSize <= 0) {
            bufferSize = 100;
        }

        if (charset == null) {
            charset = detectCharset(file);
            informationObject.setCharset(charset);
        }

        StringBuilder stringBuider = new StringBuilder();

        try {
            FileInputStream fstream = new FileInputStream(file);

            InputStreamReader is = new InputStreamReader(fstream, charset);

            br = new BufferedReader(is);

            if (offset != -1) {
                if (offset != 0) {

                    if (lastFileLength <= file.length()) {
                        br.skip(offset);
                    }
                } else {
                    offset = file.length();
                    br.skip(offset);
                }
            }
            String line = "";
            int counter = 0;

            while (line != null) {

                if (!line.equals("") && !line.equals("\r")) {

                    if (line.charAt(line.length() - 1) != '\n' || !line.substring(line.length() - 2).equals("\r\n")) {
                        stringBuider.append(line).append(System.lineSeparator());

                    } else {
                        stringBuider.append(line);
                    }
                }
                counter++;

                if (counter == bufferSize) {
                    informationObject.setElementCount(counter);
                    informationObject.getWindowTextConsole().appendText(stringBuider.toString());
                    stringBuider = new StringBuilder();
                    counter = 0;
                }
                line = br.readLine();
            }

            if (stringBuider.length() > 0) {
                informationObject.getWindowTextConsole().appendText(stringBuider.toString());
            }

            long fileLength = file.length();

            br.close();

            fstream = new FileInputStream(file);
            is = new InputStreamReader(fstream, charset);
            br = new BufferedReader(is);

            long actuallySkip = br.skip(fileLength);
            informationObject.setLastFileLength(fileLength);
            informationObject.setOffset(actuallySkip);
            br.close();

        } catch (IOException e) {
            logger.error("Error opening file", e);
            informationObject.getWindowTextConsole().insertLine(true);
            informationObject.getWindowTextConsole().insertLine(true);
            informationObject.getWindowTextConsole().appendText("Error opening file: " + e.toString());

        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
            }
        }
    }

    public InformationObject getInformationObject() {
        return informationObject;
    }

    public void setInformationObject(InformationObject informationObject) {
        this.informationObject = informationObject;
    }

}
