package net.nnwsf.handler.nocode;

import java.lang.reflect.InvocationTargetException;

import net.nnwsf.handler.AnnotatedMethodParameter;
import net.nnwsf.handler.MethodParameter;
import net.nnwsf.nocode.SchemaObject;

public class ControllerProxyNocodeDeleteImplementation extends ControllerProxyNocodeImplementation {


    public ControllerProxyNocodeDeleteImplementation(String rootPath, String method, SchemaObject schemaObject, Class<?> controllerClass) {
        super(rootPath, "/{id}", method, schemaObject, controllerClass, "Delete " + schemaObject.getTitle());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object[] parameters) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object entity = repository.findById(parameters[0]);
        repository.delete(entity);
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

    @Override
    public MethodParameter getSpecialRequestParameter(Class<?> aClass) {
        return null;
    }

    @Override
    public AnnotatedMethodParameter[] getParameters() {
        return new AnnotatedMethodParameter[] {
            new AnnotatedMethodParameter(new PathVariableImpl("id"), "id", int.class, 0),
        };
    }

}
