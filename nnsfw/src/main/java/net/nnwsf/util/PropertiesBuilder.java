package net.nnwsf.util;

import java.util.Properties;

public class PropertiesBuilder {

    private final Properties properties = new Properties();

    public PropertiesBuilder put(Object key, Object value) {
        properties.put(key, value);
        return this;
    }
    
    public Properties build() {
        return properties;
    }
}