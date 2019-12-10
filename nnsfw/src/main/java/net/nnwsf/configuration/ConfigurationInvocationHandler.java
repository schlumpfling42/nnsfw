package net.nnwsf.configuration;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import net.nnwsf.util.ReflectionHelper;

public class ConfigurationInvocationHandler implements InvocationHandler {

    private final Object object;
    private String configurationName;

    ConfigurationInvocationHandler(Object object) {
        this.object = object;
        this.configurationName = ReflectionHelper.findAnnotation(object.getClass(), ConfigurationKey.class).value();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        ConfigurationKey key = ReflectionHelper.findAnnotation(method, ConfigurationKey.class);

        Object codeValue = method.invoke(object, args);
        if(codeValue == null || codeValue.equals(method.getDefaultValue())) {
            if(key != null) {
                return ConfigurationManager.get(new String[]{configurationName, key.value()}, method.getReturnType());
            }
        }
        return codeValue;
    }
}