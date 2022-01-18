package net.nnwsf.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.nnwsf.configuration.ConfigurationManagerForTesting;
import net.nnwsf.application.annotation.DatasourceConfiguration;
import net.nnwsf.configuration.ConfigurationManager;
import net.nnwsf.persistence.annotation.Property;
import net.nnwsf.service.ServiceManager;
import net.nnwsf.service.annotation.Service;
import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.InjectionHelper;

@DatasourceConfiguration(
    jdbcDriver = "org.h2.Driver",
    jdbcUrl = "jdbc:h2:./src/test/db",
    providerClass = org.hibernate.jpa.HibernatePersistenceProvider.class,
    properties = { @Property(name="hibernate.dialect", value="org.hibernate.dialect.H2Dialect"),
        @Property(name="hibernate.hbm2ddl.auto", value="create-drop")
    }
)
public class TestPersistenceManager {


    @Service
    public static class InjectionService {
        @Inject
        TestRepository testRepository;
    }

    private InjectionService injectionService;

    @BeforeAll
    public static void setupBeforeAll() {
        Map<String, Object> appConfiguration = Map.of();
        Map<String, Object> defaultConfiguration = Map.of();
        ConfigurationManager configurationManager = new ConfigurationManagerForTesting(defaultConfiguration, appConfiguration);
        ConfigurationManagerForTesting.init(configurationManager, TestPersistenceManager.class.getClassLoader());
        ClassDiscovery.init("net.nnwsf");
        ServiceManager.init();
        PersistenceManager.init();
    }

    @BeforeEach
    public void setupEach() throws Exception {
        injectionService = InjectionHelper.getInjectable(InjectionService.class, null);
    }
    
    @Test
    public void testInsert() {
        TestEntity newTestEnity = new TestEntity();
        newTestEnity.setId(1);
        newTestEnity.setName(UUID.randomUUID().toString());
        try {
            injectionService.testRepository.save(newTestEnity);
            Optional<TestEntity> findFirst = injectionService.testRepository.findAll().stream().findFirst();
            assertEquals(true, findFirst.isPresent());
            assertEquals(newTestEnity.getName(), findFirst.get().getName());
        } finally {
            injectionService.testRepository.delete(newTestEnity);
        }
    }
}