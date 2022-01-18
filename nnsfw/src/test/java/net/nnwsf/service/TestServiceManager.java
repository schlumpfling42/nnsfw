package net.nnwsf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.nnwsf.service.annotation.Service;
import net.nnwsf.util.ClassDiscovery;

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

    @Service("test2")
    public static class TestService2 {
        public String test(String aString) {
            return "TestService2:" + aString;
        }
    }

    @BeforeAll
    public static void setup() {
        ClassDiscovery.init("net.nnwsf");
        ServiceManager.init();
    }
    
    @Test
    public void testServiceInterface() {
        TestService testService = ServiceManager.createService(TestService.class, null);
        assertEquals("TestServiceImpl:test", testService.test("test"));
    }

    @Test
    public void testServiceInterface2Test() {
        TestService testService = ServiceManager.createService(TestService.class, "test2");
        assertEquals("TestServiceImpl2:test", testService.test("test"));
    }

    @Test
    public void testService2Test() {
        TestService2 testService = ServiceManager.createService(TestService2.class, null);
        assertEquals("TestService2:test", testService.test("test"));
    }
}
