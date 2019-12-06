package net.nnwsf.util;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;

public class ProxyUtil {

    public static Object createProxy(Class<?> aClass, Callback callback) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(aClass);
        enhancer.setCallback(callback);
        return enhancer.create();
    }
}