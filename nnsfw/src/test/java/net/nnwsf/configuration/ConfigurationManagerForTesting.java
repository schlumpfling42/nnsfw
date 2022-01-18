package net.nnwsf.configuration;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationManagerForTesting extends ConfigurationManager {

    public static void init(ConfigurationManager anInstance, ClassLoader applicationClassLoader) {
        ConfigurationManager.init(anInstance, applicationClassLoader);
    }

    Map<String, Object> defaultConfiguration;
    Map<String, Object> appConfiguration;

    public ConfigurationManagerForTesting(Map<String, Object> defaultConfiguration, Map<String, Object> appConfiguration) {
        this.defaultConfiguration = defaultConfiguration;
        this.appConfiguration = appConfiguration;
    }

    @Override
    Map<String, Object> loadApplicationConfiguration(ClassLoader applicationClassLoader) {
        return new HashMap<>(appConfiguration);
    }

    @Override
    Map<String, Object> loadDefaultConfiguration() {
        return new HashMap<>(defaultConfiguration);
    }
}
