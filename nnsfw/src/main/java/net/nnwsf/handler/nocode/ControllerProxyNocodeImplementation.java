package net.nnwsf.handler.nocode;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.nnwsf.application.Constants;
import net.nnwsf.controller.annotation.RequestParameter;
import net.nnwsf.controller.annotation.PathVariable;
import net.nnwsf.handler.AnnotatedMethodParameter;
import net.nnwsf.handler.EndpointProxy;
import net.nnwsf.handler.MethodParameter;
import net.nnwsf.handler.URLMatcher;
import net.nnwsf.nocode.NocodeManager;
import net.nnwsf.nocode.SchemaObject;
import net.nnwsf.persistence.PersistenceManager;
import net.nnwsf.persistence.PersistenceRepository;

public abstract class ControllerProxyNocodeImplementation implements EndpointProxy {

    protected class RequestParameterImpl implements RequestParameter{

        private final String value;

        RequestParameterImpl(String value) {
            this.value = value;
        }

        public Class<? extends Annotation> annotationType() {
            return RequestParameter.class;
        }

        @Override
        public String value() {
            return value;
        }
        
    }

    protected class PathVariableImpl implements PathVariable{

        private final String value;

        PathVariableImpl(String value) {
            this.value = value;
        }

        public Class<? extends Annotation> annotationType() {
            return PathVariable.class;
        }

        @Override
        public String value() {
            return value;
        }
        
    }

    private final SchemaObject schemaObject;
    private final URLMatcher urlMatcher;
    private final String rootPath;
    private final String httpMethod;
    protected final Class<?> entityClass;
    protected final PersistenceRepository repository;

    ControllerProxyNocodeImplementation(String rootPath, String pathPostFix, String method, SchemaObject schemaObject) {
        this.schemaObject = schemaObject;
        this.urlMatcher = new URLMatcher(method, (rootPath + "/" + schemaObject.getTitle() + (pathPostFix == null ? "" : pathPostFix)).toLowerCase().replaceAll("/+", "/"));
        this.rootPath = rootPath;
        this.httpMethod = method;
        this.entityClass = NocodeManager.getEntityClass(schemaObject).getFirst();
        Class<PersistenceRepository> repositoryClass = NocodeManager.getRepositoryClass(schemaObject);
        repository = (PersistenceRepository)PersistenceManager.createRepository(repositoryClass);
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
        return Arrays.asList(urlMatcher.getPathElements());
    }

    @Override
    public String toString() {
        return "ControllerProxy [class=" + schemaObject.getTitle() + "]";
    }

    @Override
    public int getParametersCount() {
        return 0;
    }

    @Override
    public URLMatcher getUrlMatcher() {
        return urlMatcher;
    }

    @Override
    public String getPath() {
        return (rootPath + "/" + schemaObject.getTitle()).toLowerCase().replaceAll("/+", "/");
    }

    @Override
    public String getHttpMethod() {
        return httpMethod;
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
