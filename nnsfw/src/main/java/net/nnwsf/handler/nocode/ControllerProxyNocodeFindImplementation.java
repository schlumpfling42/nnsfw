package net.nnwsf.handler.nocode;

import java.lang.reflect.InvocationTargetException;

import io.smallrye.mutiny.Uni;
import net.nnwsf.handler.AnnotatedMethodParameter;
import net.nnwsf.nocode.SchemaObject;
import net.nnwsf.query.QueryParser;
import net.nnwsf.resource.Page;
import net.nnwsf.resource.PageRequest;

public class ControllerProxyNocodeFindImplementation extends ControllerProxyNocodeImplementation {


    /**
     *
     */
    private static final AnnotatedMethodParameter[] ANNOTATED_METHOD_PARAMETERS = new AnnotatedMethodParameter[] {
        new AnnotatedMethodParameter(new RequestParameterImpl("page"), "page", int.class, 0),
        new AnnotatedMethodParameter(new RequestParameterImpl("size"), "size", int.class, 1),
        new AnnotatedMethodParameter(new RequestParameterImpl("search"), "search", String.class, 2),
    };

    public ControllerProxyNocodeFindImplementation(String rootPath, String method, SchemaObject schemaObject, Class<?> controllerClass) {
        super(rootPath, null,  method, schemaObject, controllerClass, "Find " + schemaObject.getTitle());
    }

    @Override
    public Uni<?> invoke(Object[] parameters) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return executeWithSession(false, () -> repository.find(PageRequest.of((Integer)parameters[0], (Integer)parameters[1]), QueryParser.parseString((String)parameters[2])));
    }


    @Override
    public int getParametersCount() {
        return 3;
    }

    @Override
    public Class<?> getReturnType() {
        return Page.class;
    }

    @Override
    public Class<?>[] getGenericReturnTypes() {
        return new Class[] {entityClass};
    }

    @Override
    public AnnotatedMethodParameter[] getParameters() {
        return ANNOTATED_METHOD_PARAMETERS;
    }

}
