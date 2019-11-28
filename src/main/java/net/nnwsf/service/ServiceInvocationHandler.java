package net.nnwsf.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ServiceInvocationHandler implements InvocationHandler {

    private final Object service;

    public ServiceInvocationHandler(Object aService) {
        this.service = aService;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(service, args);
    }

}
