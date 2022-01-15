package net.nnwsf.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.nnwsf.application.annotation.ServerConfiguration;

public class TestConfigurationManager {
    
    @ServerConfiguration(resourcePath = "testPath")
    static class TestApplication {

    }

    private void setupConfigurationManager(Map<String, Object> defaultConfiguration, Map<String, Object> appConfiguration) {
        ConfigurationManager configurationManager = new ConfigurationManagerForTesting(defaultConfiguration, appConfiguration);
        ConfigurationManager.init(configurationManager, getClass().getClassLoader());
    }

    @Test
    @DisplayName("Ensure that application configuration overwrites default")
    public void testAppConfigurationOverwritingDefault() {
        Map<String, Object> defaultConfiguration = Map.of("server", Map.of("port", 5000, "hostname", "localhost"));
        Map<String, Object> appConfiguration = Map.of("server", Map.of("port", 8000));

        setupConfigurationManager(defaultConfiguration, appConfiguration);

        ServerConfiguration config = ConfigurationManager.apply(TestApplication.class.getAnnotation(ServerConfiguration.class));

        assertEquals(8000, config.port());
        assertEquals("localhost", config.hostname());
        assertEquals("testPath", config.resourcePath());

    }
}
