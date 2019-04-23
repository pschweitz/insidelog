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

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.SortedMap;

import org.mozilla.universalchardet.UniversalDetector;

public class CharsetDetect extends Thread {

    private static final Logger logger = Logger.getLogger(CharsetDetect.class);

    private InputStream in = null;
    private InformationObject informationObject = null;
    private long maxChanlangedBytes = 4096;

    public Charset charset = StandardCharsets.US_ASCII;

    public CharsetDetect(InputStream in, InformationObject informationObject, long maxChanlangedBytes) {

        this.in = in;
        this.informationObject = informationObject;
        this.maxChanlangedBytes = maxChanlangedBytes;
    }

    public void run() {
        byte[] buf = new byte[4096];

        SortedMap<String, Charset> charsetMap = Charset.availableCharsets();

        if (informationObject.getCharset() == null) {
            try {

                UniversalDetector detector = new UniversalDetector(null);
                String encoding = null;
                int nread;
                long nreadtotal = 0;
                while ((nread = in.read(buf)) > 0 && !detector.isDone() && nreadtotal < maxChanlangedBytes) {
                    detector.handleData(buf, 0, nread);
                    nreadtotal += nread;
                }

                detector.dataEnd();

                encoding = detector.getDetectedCharset();
                if (encoding != null) {
                    logger.info("Detected charset = " + encoding);

                    if (charsetMap.containsKey(encoding.toUpperCase())) {

                        logger.info("Detected charset is in supported list !");
                        charset = charsetMap.get(encoding.toUpperCase());
                        this.informationObject.setCharset(charset);
                        InSideLog.saveTreeToFile();
                    }
                } else {
                    logger.warning("No charset detected. Using default: " + charset.name());
                }

                detector.reset();
                in.close();

            } catch (Exception e) {
            } finally {
                try {
                    in.close();
                } catch (Exception ex) {
                }
            }
        }
    }
}
