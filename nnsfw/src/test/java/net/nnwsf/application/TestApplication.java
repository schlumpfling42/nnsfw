package net.nnwsf.application;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import io.vertx.mutiny.core.Vertx;
import net.nnwsf.application.annotation.DatasourceConfiguration;
import net.nnwsf.application.annotation.ServerConfiguration;
import net.nnwsf.configuration.ConfigurationManager;
import net.nnwsf.configuration.ConfigurationManagerForTesting;
import net.nnwsf.nocode.NocodeManager;
import net.nnwsf.persistence.DatasourceManager;
import net.nnwsf.persistence.PersistenceManager;
import net.nnwsf.persistence.TestPersistenceManager;
import net.nnwsf.service.ServiceManager;
import net.nnwsf.util.ClassDiscovery;

public class TestApplication {
    
    @ServerConfiguration(port=9999, hostname="localhost")
    @DatasourceConfiguration
    class Application {

    }

    @Test
    public void testStartApplicationServer() {
        ApplicationServer.start(Application.class);
    }

    @BeforeAll
    public static void init() {
        PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:11-alpine")
        .withDatabaseName("postgres")
        .withUsername("postgres")
        .withPassword("postgres");
  
        postgreSQLContainer.start();

        Map<String, Object> defaultConfiguration = Map.of();
        Map<String, Object> appConfiguration = Map.of("datasource", Map.of("default", Map.of(
            "providerClass", "org.hibernate.jpa.HibernatePersistenceProvider",
            "jdbcDriver", "org.postgresql.Driver",
            "jdbcUrl", "jdbc:postgresql://localhost:" + postgreSQLContainer.getMappedPort(5432) + "/postgres",
            "user", "postgres",
            "password", "postgres",
            "properties", Map.of("hibernate.dialect", "org.hibernate.dialect.PostgreSQL95Dialect", "hibernate.hbm2ddl.auto", "create-drop"))));
        ConfigurationManager configurationManager = new ConfigurationManagerForTesting(defaultConfiguration, appConfiguration);
        ConfigurationManagerForTesting.init(configurationManager, TestPersistenceManager.class.getClassLoader());
        ClassDiscovery.init("net.nnwsf");
        ServiceManager.init();
        DatasourceManager.init();
        NocodeManager.init(TestPersistenceManager.class.getClassLoader(), null);
        PersistenceManager.init(Vertx.vertx());
    }
}
