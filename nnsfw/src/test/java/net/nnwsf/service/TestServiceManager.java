package net.nnwsf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.mutiny.core.Vertx;
import net.nnwsf.configuration.ConfigurationManager;
import net.nnwsf.nocode.NocodeManager;
import net.nnwsf.persistence.DatasourceManager;
import net.nnwsf.persistence.PersistenceManager;
import net.nnwsf.service.annotation.Service;
import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.InjectionHelper;

public class TestServiceManager {

    @Service
    public static interface TestService {
        public String test(String aString);
    }

    public static class TestServiceImpl implements TestService {
        public String test(String aString) {
            return "TestServiceImpl:" + aString;
        }
    }

    @Service("test2")
    public static class TestServiceImpl2 implements TestService {
        public String test(String aString) {
            return "TestServiceImpl2:" + aString;
        }
    }

    @Service
    public static class TestService2 {
        public String test(String aString) {
            return "TestService2:" + aString;
        }
    }

    @BeforeAll
    public static void setup() {
        ClassDiscovery.init("net.nnwsf");
        ConfigurationManager.init(TestServiceManager.class.getClassLoader());
        ServiceManager.init();
        DatasourceManager.init();
        NocodeManager.init(null, null);
        PersistenceManager.init(Vertx.vertx());
    }

    @Service
    public static class InjectionService {
        @Inject
        TestService testService;

        @Inject
        @Named("test2")
        TestService testServiceNamed;
        
        @Inject
        TestService2 testService2;

        public String test(String aString) {
            return "TestService2:" + aString;
        }
    }

    private InjectionService injectionService;

    @BeforeEach
    public void setupEach() throws Exception {
        injectionService = InjectionHelper.getInjectable(InjectionService.class, null);
    }
    
    @Test
    public void testServiceInterface() {
        TestService testService = injectionService.testService;
        assertEquals("TestServiceImpl:test", testService.test("test"));
    }

    @Test
    public void testServiceInterface2Test() {
        TestService testService = injectionService.testServiceNamed;
        assertEquals("TestServiceImpl2:test", testService.test("test"));
    }

    @Test
    public void testService2Test() {
        TestService2 testService = injectionService.testService2;
        assertEquals("TestService2:test", testService.test("test"));
    }
}
