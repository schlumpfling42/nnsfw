package net.nnwsf.util;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

public class ProxyUtil {

    public static <T> T createProxy(Class<T> aClass, Class<?> interceptor) {
        try {
            return new ByteBuddy()
                .subclass(aClass)
                .method(ElementMatchers.isDeclaredBy(aClass))
                .intercept(MethodDelegation.to(interceptor))
                .make()
                .load(aClass.getClassLoader()).getLoaded()
                .getDeclaredConstructor(new Class[0])
                .newInstance(new Object[0]);
        } catch(Exception e) {
            throw new RuntimeException("Unable to create proxy for class: " + aClass, e);
        }
    }

    public static <T> T createProxy(Class<T> aClass, Object interceptor) {
        try {
            return new ByteBuddy()
                .subclass(aClass)
                .method(ElementMatchers.any())
                .intercept(MethodDelegation.to(interceptor))
                .make()
                .load(aClass.getClassLoader()).getLoaded()
                .getDeclaredConstructor(new Class[0])
                .newInstance(new Object[0]);
        } catch(Exception e) {
            throw new RuntimeException("Unable to create proxy for class: " + aClass, e);
        }
    }
}