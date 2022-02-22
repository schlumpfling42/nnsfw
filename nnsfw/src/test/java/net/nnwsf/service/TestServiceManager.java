package net.nnwsf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import net.nnwsf.application.TestApplication;
import net.nnwsf.service.annotation.Service;
import net.nnwsf.util.InjectionHelper;

public class TestServiceManager {

    @Service
    public static interface TestService {
        public Uni<String> test(String aString);
    }

    public static class TestServiceImpl implements TestService {
        public Uni<String> test(String aString) {
            return Uni.createFrom().item("TestServiceImpl:" + aString);
        }
    }

    @Service("test2")
    public static class TestServiceImpl2 implements TestService {
        public Uni<String> test(String aString) {
            return Uni.createFrom().item("TestServiceImpl2:" + aString);
        }
    }

    @Service
    public static class TestService2 {
        public Uni<String> test(String aString) {
            return Uni.createFrom().item("TestService2:" + aString);
        }
    }

    @BeforeAll
    public static void setup() {
        TestApplication.init();
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
    }

    private InjectionService injectionService;

    @BeforeEach
    public void setupEach() throws Exception {
        injectionService = InjectionHelper.getInjectable(InjectionService.class, null);
    }
    
    @Test
    public void testServiceInterface() {
        TestService testService = injectionService.testService;
        assertEquals("TestServiceImpl:test", testService.test("test").await().indefinitely());
    }

    @Test
    public void testServiceInterface2Test() {
        TestService testService = injectionService.testServiceNamed;
        assertEquals("TestServiceImpl2:test", testService.test("test").await().indefinitely());
    }

    @Test
    public void testService2Test() {
        TestService2 testService = injectionService.testService2;
        assertEquals("TestService2:test", testService.test("test").await().indefinitely());
    }
}
