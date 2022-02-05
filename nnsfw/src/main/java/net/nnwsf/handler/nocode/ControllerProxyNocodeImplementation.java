package net.nnwsf.handler.nocode;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.nnwsf.application.Constants;
import net.nnwsf.handler.AnnotatedMethodParameter;
import net.nnwsf.handler.EndpointProxy;
import net.nnwsf.handler.MethodParameter;
import net.nnwsf.handler.URLMatcher;
import net.nnwsf.nocode.SchemaElement;

public class ControllerProxyNocodeImplementation implements EndpointProxy {

    private final SchemaElement schemaElement;
    private final URLMatcher urlMatcher;
    private final String rootPath;
    private final String method;

    ControllerProxyNocodeImplementation(String rootPath, String method, SchemaElement schemaElement) {
        this.schemaElement = schemaElement;
        this.urlMatcher = new URLMatcher(method, (rootPath + "/" + schemaElement.getTitle()).toLowerCase().replaceAll("/+", "/"));
        this.rootPath = rootPath;
        this.method = method;
    }

    public Collection<Annotation> getAnnotations() {
        return Collections.emptyList();
    }

    public String getContentType() {
        return Constants.CONTENT_TYPE_APPLICATION_JSON;
    }

    public AnnotatedMethodParameter[] getParameters() {
        return new AnnotatedMethodParameter[0];
    }

    public MethodParameter[] getSpecialParameters() {
        return new MethodParameter[0];
    }

    public List<String> getPathElements() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "ControllerProxy [class=" + schemaElement.getTitle() + "]";
    }

    @Override
    public int getParametersCount() {
        return 0;
    }

    @Override
    public Object invoke(Object[] parameters) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return null;
    }

    @Override
    public URLMatcher getUrlMatcher() {
        return urlMatcher;
    }

    @Override
    public String getPath() {
        return (rootPath + "/" + schemaElement.getTitle()).toLowerCase().replaceAll("/+", "/");
    }

    @Override
    public String getHttpMethod() {
        return method;
    }

    @Override
    public Class<?> getControllerClass() {
        return NocodeController.class;
    }

    @Override
    public MethodParameter getSpecialRequestParameter(Class<?> aClass) {
        return null;
    }

    @Override
    public Class<?> getReturnType() {
        return null;
    }

    @Override
    public Class<?>[] getGenericReturnTypes() {
        return null;
    }
}
