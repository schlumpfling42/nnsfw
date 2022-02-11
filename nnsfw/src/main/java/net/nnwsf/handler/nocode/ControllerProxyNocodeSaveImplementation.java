package net.nnwsf.handler.nocode;

import java.lang.reflect.InvocationTargetException;

import net.nnwsf.controller.annotation.RequestBody;
import net.nnwsf.handler.AnnotatedMethodParameter;
import net.nnwsf.handler.MethodParameter;
import net.nnwsf.nocode.SchemaObject;
import net.nnwsf.util.ReflectionHelper;

public class ControllerProxyNocodeSaveImplementation extends ControllerProxyNocodeImplementation {


    public ControllerProxyNocodeSaveImplementation(String rootPath, String method, SchemaObject schemaObject) {
        super(rootPath, "/{id}", method, schemaObject);
    }

    @Override
    public Object invoke(Object[] parameters) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object entity = repository.findById(parameters[0]);
        if(entity == null) {
            throw new RuntimeException("Entity of type " + schemaObject.getTitle() + " not found for id " + parameters[0]);
        }
        ReflectionHelper.copy(entity, parameters[1], fields);
        return repository.save(entity);
    }

    @Override
    public int getParametersCount() {
        return 2;
    }

    @Override
    public Class<?> getReturnType() {
        return entityClass;
    }

    @Override
    public MethodParameter getSpecialRequestParameter(Class<?> aClass) {
        if(RequestBody.class.isAssignableFrom(aClass)) {
            return new MethodParameter("body", entityClass, 1);
        }
        return null;
    }

    @Override
    public AnnotatedMethodParameter[] getParameters() {
        return new AnnotatedMethodParameter[] {
            new AnnotatedMethodParameter(new PathVariableImpl("id"), "id", int.class, 0),
            null
        };
    }

}
