package net.nnwsf.handler.nocode;

import java.lang.reflect.InvocationTargetException;

import io.smallrye.mutiny.Uni;
import net.nnwsf.handler.AnnotatedMethodParameter;
import net.nnwsf.handler.MethodParameter;
import net.nnwsf.nocode.SchemaObject;

public class ControllerProxyNocodeDeleteImplementation extends ControllerProxyNocodeImplementation {


    public ControllerProxyNocodeDeleteImplementation(String rootPath, String method, SchemaObject schemaObject, Class<?> controllerClass) {
        super(rootPath, "/:id", method, schemaObject, controllerClass, "Delete " + schemaObject.getTitle());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Uni<?> invoke(Object[] parameters) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return executeWithSession(true, () -> repository.findById(parameters[0])
            .chain(entity -> repository.delete(entity)));
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
