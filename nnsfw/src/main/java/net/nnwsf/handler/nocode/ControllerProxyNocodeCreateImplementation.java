package net.nnwsf.handler.nocode;

import java.lang.reflect.InvocationTargetException;

import io.smallrye.mutiny.Uni;
import net.nnwsf.controller.annotation.RequestBody;
import net.nnwsf.handler.MethodParameter;
import net.nnwsf.nocode.SchemaObject;

public class ControllerProxyNocodeCreateImplementation extends ControllerProxyNocodeImplementation {


    public ControllerProxyNocodeCreateImplementation(String rootPath, String method, SchemaObject schemaObject, Class<?> controllerClass) {
        super(rootPath, null, method, schemaObject, controllerClass, "Create " + schemaObject.getTitle());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Uni<?> invoke(Object[] parameters) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return executeWithSession(true, () -> repository.save(parameters[0]));
    }

    @Override
    public MethodParameter getSpecialRequestParameter(Class<?> aClass) {
        if(RequestBody.class.isAssignableFrom(aClass)) {
            return new MethodParameter("body", entityClass, 0);
        }
        return null;
    }

    @Override
    public int getParametersCount() {
        return 1;
    }

    @Override
    public Class<?> getReturnType() {
        return entityClass;
    }

}
