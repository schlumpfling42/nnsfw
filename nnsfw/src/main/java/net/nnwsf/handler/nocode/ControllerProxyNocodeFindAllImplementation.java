package net.nnwsf.handler.nocode;

import java.lang.reflect.InvocationTargetException;

import net.nnwsf.nocode.SchemaObject;

public class ControllerProxyNocodeFindAllImplementation extends ControllerProxyNocodeImplementation {


    public ControllerProxyNocodeFindAllImplementation(String rootPath, String method, SchemaObject schemaObject, Class<?> controllerClass) {
        super(rootPath, null, method, schemaObject, controllerClass);
    }

    @Override
    public Object invoke(Object[] parameters) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return repository.findAll();
    }

}
