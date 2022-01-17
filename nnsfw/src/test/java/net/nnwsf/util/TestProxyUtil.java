package net.nnwsf.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;



public class TestProxyUtil {

    public static class TestObjectClass {
        public String test(String test) {
            return "test";
        }
    }

    public static interface TestObjectInterface {
        public String test(String test);
    }
    
    public static class TimingInterceptor {
        @RuntimeType
        public static Object intercept(@Origin Method method, 
                                       @SuperCall Callable<?> callable) throws Exception {
          long start = System.currentTimeMillis();
          try {
            return callable.call();
          } finally {
            System.out.println(method + " took " + (System.currentTimeMillis() - start));
          }
        }
    }

    public static class OutputInterceptor {
        @RuntimeType
        public Object intercept(@Origin Method method, 
                                       @AllArguments Object[] args) throws Exception {
          long start = System.currentTimeMillis();
          try {
            return args[0];
          } finally {
            System.out.println(method + " took " + (System.currentTimeMillis() - start));
          }
        }
    }

    @Test
    public void testProxyClass() {
        String result = ProxyUtil.createProxy(TestObjectClass.class, TimingInterceptor.class).test("foo");
        assertEquals("test", result);
    }

    @Test
    public void testProxyInterface() {
        String result = ProxyUtil.createProxy(TestObjectInterface.class, new OutputInterceptor()).test("foo");
        assertEquals("foo", result);
    }
}
