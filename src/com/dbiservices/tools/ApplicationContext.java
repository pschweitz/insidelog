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

package com.dbiservices.tools;

/**
 *
 * @author  Philippe Schweitzer
 * @version 1.1
 * @since   16.11.2015
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Philippe Schweitzer
 * @param <K>
 * @param <V>
 */
public final class ApplicationContext<K, V> {

    private static final Logger logger = Logger.getLogger(ApplicationContext.class);

    private static final String propertiesFilename = "etc/tail.properties";

    private static final ApplicationContext<String, Object> instance = new ApplicationContext();
    private ConcurrentHashMap<K, V> properties = new ConcurrentHashMap();

    private enum PropertyType {

        LOGLEVEL("logger.level", String.class),
        BUFFERSIZE("tail.bufferSize", Integer.class),
        FREQUENCYINTERVAL("tail.frequencyInterval", Long.class),
        DISPLAYCOLOR("tail.displayColor", Boolean.class);

        /* Long.class Boolean.class Double.class*/
        String propertyName;
        Class c;

        PropertyType(String propertyName, Class c) {
            this.propertyName = propertyName;
            this.c = c;
        }

        private static Class getClass(String propertyName) {
            Class result = String.class;
            boolean propertyNameRecognized = false;

            for (PropertyType property : values()) {
                if (propertyName.startsWith(property.propertyName)) {
                    result = property.c;
                    propertyNameRecognized = true;
                    break;
                }
            }

            if (!propertyNameRecognized) {
                logger.warning("Property \"" + propertyName + "\" not recognized");
            }

            return result;
        }

        public static boolean isRecognized(String propertyName) {
            boolean result = false;

            for (PropertyType property : values()) {
                if (propertyName.startsWith(property.propertyName)) {
                    result = true;
                    break;
                }
            }

            return result;
        }

        public static boolean validatePropertyType(String propertyName, String propertyValue) {
            boolean result = true;

            Class c = getClass(propertyName);
            String className = c.getSimpleName();

            if (className.equals("Integer") || className.equals("Long") || className.equals("Double") || className.equals("Short") || className.equals("Float")) {
                
                Method method;
                try {
                    method = c.getMethod("valueOf", String.class);
                    method.invoke(null, propertyValue);

                } catch (NoSuchMethodException e) {
                    logger.error("Type error with property \"" + propertyName + "\" for value: \"" + propertyValue + "\", value must be instance of " + c.getSimpleName(), e);
                    result = false;

                } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    logger.error("Type error with property \"" + propertyName + "\" for value: \"" + propertyValue + "\", value must be instance of " + c.getSimpleName(), e);
                    result = false;
                }
            } else if (className.equals("Boolean")) {
                propertyValue = propertyValue.toLowerCase();
                if (!(propertyValue.equals("true") || propertyValue.equals("false"))) {
                    logger.error("Type error with property \"" + propertyName + "\" for value: \"" + propertyValue + "\", value must be \"true\" or \"false\"");
                }
            }

            return result;
        }

    }

    private ApplicationContext() {
        logger.trace("START ApplicationContext()");

        properties = new ConcurrentHashMap();
        loadProperties();

        logger.trace("END ApplicationContext()");
    }

    public static ApplicationContext getInstance() {
        logger.trace("START ststic getInstance()");

        logger.trace("END static getInstance()");

        return instance;
    }

    public void put(K key, V value) {
        logger.trace("START put(K, V)");

        this.properties.put(key, value);

        logger.trace("END put(K, V)");
    }

    public void remove(K key) {
        logger.trace("START remove(K)");

        this.properties.remove(key);

        logger.trace("END remove(K)");
    }

    public void replace(K key, V value) {
        logger.trace("START replace(K, V)");

        this.properties.replace(key, value);

        logger.trace("END replace(K, V)");
    }

    public boolean contains(V value) {
        logger.trace("START contains(V)");

        logger.trace("END contains(V)");

        return this.properties.contains(value);
    }

    public boolean containsKey(K key) {
        logger.trace("START containsKey(K)");

        logger.trace("END containsKey(K)");

        return this.properties.containsKey(key);
    }

    public V get(K key) {
        logger.trace("START get(K)");

        logger.trace("END get(K)");

        return (V) this.properties.get(key);
    }

    public Enumeration<V> elements() {
        logger.trace("START elements()");

        logger.trace("END elements()");

        return this.properties.elements();
    }

    public Enumeration<K> keys() {
        logger.trace("START keys()");

        logger.trace("END keys()");

        return this.properties.keys();
    }

    public ConcurrentHashMap.KeySetView<K, V> keySet() {
        logger.trace("START keySet()");

        logger.trace("END keySet()");

        return this.properties.keySet();
    }

    public String getString(K key) {
        logger.trace("START getString(K)");

        String result = "";

        if (this.properties.containsKey(key)) {
            result = this.properties.get(key).toString();
        } else {
            logger.error("Application context does not contains property: \"" + key.toString() + "\"");
        }

        logger.trace("END getString(K)");

        return result;
    }

    public Integer getInteger(K key) {
        logger.trace("START getInteger(K)");

        Integer result = 0;

        if (this.properties.containsKey(key)) {
            String value = "";
            try {
                value = this.properties.get(key).toString();
                result = Integer.valueOf(value);

            } catch (IllegalArgumentException e) {
                logger.error("Type error with property \"" + key.toString() + "\" for value: \"" + value + "\", value must be instance of " + Integer.class.getSimpleName(), e);
            }
        } else {
            logger.error("Application context does not contains property: \"" + key.toString() + "\"");
        }

        logger.trace("END getInteger(K)");

        return result;
    }

    public Long getLong(K key) {
        logger.trace("START getLong(K)");

        Long result = 0L;

        if (this.properties.containsKey(key)) {
            String value = "";
            try {
                value = this.properties.get(key).toString();
                result = Long.valueOf(value);

            } catch (IllegalArgumentException e) {
                logger.error("Type error with property \"" + key.toString() + "\" for value: \"" + value + "\", value must be instance of " + Long.class.getSimpleName(), e);
            }
        } else {
            logger.error("Application context does not contains property: \"" + key.toString() + "\"");
        }

        logger.trace("END getLong(K)");

        return result;
    }

    public Double getDouble(K key) {
        logger.trace("START getDouble(K)");

        Double result = 0.0;

        if (this.properties.containsKey(key)) {
            String value = "";
            try {
                value = this.properties.get(key).toString();
                result = Double.valueOf(value);

            } catch (IllegalArgumentException e) {
                logger.error("Type error with property \"" + key.toString() + "\" for value: \"" + value + "\", value must be instance of " + Double.class.getSimpleName(), e);
            }
        } else {
            logger.error("Application context does not contains property: \"" + key.toString() + "\"");
        }

        logger.trace("END getDouble(K)");

        return result;
    }

    public Boolean getBoolean(K key) {
        logger.trace("START getBoolean(K)");

        Boolean result = false;

        if (this.properties.containsKey(key)) {
            String value = "";
            try {
                value = this.properties.get(key).toString();
                value = value.toLowerCase();
                if (!(value.equals("true") || value.equals("false"))) {
                    logger.error("Type error with property \"" + key.toString() + "\" for value: \"" + value + "\", value must be \"true\" or \"false\"");
                }
                result = Boolean.valueOf(value);

            } catch (IllegalArgumentException e) {
                logger.error("Type error with property \"" + key.toString() + "\" for value: \"" + value + "\", value must be instance of " + Boolean.class.getSimpleName(), e);
            }
        } else {
            logger.error("Application context does not contains property: \"" + key.toString() + "\"");
        }

        logger.trace("END getBoolean(K)");

        return result;
    }

    public void loadProperties() {
        logger.trace("START loadProperties()");

        loadProperties(ApplicationContext.propertiesFilename);

        logger.trace("END loadProperties()");
    }

    private synchronized void loadProperties(String propertiesFilename) {
        logger.trace("START loadProperties(String)");

        Properties propertiesSource = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(propertiesFilename);

            propertiesSource.load(input);
            properties = new ConcurrentHashMap();

            for (K key : (Set<K>) propertiesSource.keySet()) {
                this.properties.put(key, (V) propertiesSource.getProperty(key.toString()));
            }

            logger.debug("Application properties load:");
            for (K key : this.properties.keySet()) {
                logger.debug("property[\"" + key.toString() + "\"] = " + this.properties.get(key).toString());

                boolean validatePropertytype = PropertyType.validatePropertyType(key.toString(), this.properties.get(key).toString());
                if (!validatePropertytype) {
                    logger.error("Error loading application properties file: \"" + propertiesFilename + "\"");
                    return;
                }
            }

        } catch (IOException e) {
            logger.error("Error loading application properties file: \"" + propertiesFilename + "\"", e);

        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.error("Error closing Application properties file: \"" + propertiesFilename + "\"", e);
                }
            }
        }

        logger.trace("END loadProperties(String)");
    }

    public void storeProperties() {
        logger.trace("START storeProperties()");

        storeProperties(ApplicationContext.propertiesFilename);

        logger.trace("END storeProperties()");
    }

    private synchronized void storeProperties(String propertiesFilename) {
        logger.trace("START storeProperties(String)");

        Properties propertiesDestination = new Properties();
        OutputStream output = null;

        try {
            output = new FileOutputStream(propertiesFilename);

            logger.debug("Application properties store:");
            for (K key : this.properties.keySet()) {
                logger.debug("property[\"" + key.toString() + "\"] = " + this.properties.get(key));

                boolean validatePropertytype = PropertyType.validatePropertyType(key.toString(), this.properties.get(key).toString());
                if (!validatePropertytype) {
                    logger.error("Error writing application properties file: \"" + propertiesFilename + "\"");
                    return;
                }
                if (PropertyType.isRecognized(key.toString())) {
                    propertiesDestination.put(key, String.valueOf(this.properties.get(key)));
                }
            }

            propertiesDestination.store(output, null);

        } catch (FileNotFoundException e) {
            logger.error("Error writing application properties file: \"" + propertiesFilename + "\"", e);

        } catch (IOException e) {
            logger.error("Error writing application properties file: \"" + propertiesFilename + "\"", e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    logger.error("Error closing application properties file: \"" + propertiesFilename + "\"", e);
                }
            }
        }

        logger.trace("END storeProperties(String)");
    }

    public static String getPropertiesFilename() {
        return propertiesFilename;
    }
}
