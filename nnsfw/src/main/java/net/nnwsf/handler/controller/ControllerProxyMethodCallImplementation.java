package net.nnwsf.handler.controller;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.smallrye.mutiny.Uni;
import net.nnwsf.controller.annotation.AuthenticatedUser;
import net.nnwsf.controller.annotation.RequestBody;
import net.nnwsf.handler.AnnotatedMethodParameter;
import net.nnwsf.handler.EndpointProxy;
import net.nnwsf.handler.MethodParameter;
import net.nnwsf.util.ReflectionHelper;

public class ControllerProxyMethodCallImplementation implements EndpointProxy {

    private final Collection<Annotation> annotations;
    private final Method method;
    private final String contentType;
    private final Object instance;
    private final AnnotatedMethodParameter[] annotatedMethodParameters;
    private final MethodParameter[] specialMethodParameters;
    private String httpMethod;
    private final String path;
    private Map<Class<?>, MethodParameter> specialRequestParameters = new HashMap<>();
    private final Class<?> returnType;
    private final Class<?>[] genericReturnTypes;

    ControllerProxyMethodCallImplementation(
        Object instance, 
        String httpMethod,
        Collection<Annotation> annotations, 
        Method method, 
        String contentType, 
        AnnotatedMethodParameter[] annotatedMethodParameters, 
        MethodParameter[] specialMethodParameters,
        String path) {
        this.instance = instance;
        this.httpMethod = httpMethod;
        this.method = method;
        this.contentType = contentType;
        this.annotatedMethodParameters = annotatedMethodParameters;
        this.specialMethodParameters = specialMethodParameters;
        this.path = path;
        this.annotations = annotations;

        for(int i = 0; i<specialMethodParameters.length; i++) {
            MethodParameter specialMethodParameter = specialMethodParameters[i];
            if(specialMethodParameter != null) {
                if(specialMethodParameter instanceof AnnotatedMethodParameter) {
                    AnnotatedMethodParameter annotatedSpecialMethodParameter = (AnnotatedMethodParameter)specialMethodParameter;
                    if(annotatedSpecialMethodParameter.getAnnotation().annotationType().isAssignableFrom(RequestBody.class)) {
                        specialRequestParameters.put(RequestBody.class, specialMethodParameter);
                    }
                }
                if(specialMethodParameter instanceof AnnotatedMethodParameter) {
                    AnnotatedMethodParameter annotatedSpecialMethodParameter = (AnnotatedMethodParameter)specialMethodParameter;
                    if(annotatedSpecialMethodParameter.getAnnotation().annotationType().isAssignableFrom(AuthenticatedUser.class)) {
                        specialRequestParameters.put(AuthenticatedUser.class, specialMethodParameter);
                    }
                }
            }
        }
        if(void.class.equals(method.getReturnType())) {
            this.returnType = null;
            this.genericReturnTypes = null;
        } else {
            this.returnType = method.getReturnType();
            this.genericReturnTypes = ReflectionHelper.getGenericTypes(method);
        }
    }

    public Collection<Annotation> getAnnotations() {
        return annotations;
    }

    public String getContentType() {
        return contentType;
    }

    public AnnotatedMethodParameter[] getParameters() {
        return annotatedMethodParameters;
    }

    public MethodParameter[] getSpecialParameters() {
        return specialMethodParameters;
    }

    @Override
    public String toString() {
        return "ControllerProxy [class=" + instance.getClass() + ", method=" + method + ", parameterNames="
                + Arrays.toString(annotatedMethodParameters) + "]";
    }

    @Override
    public int getParametersCount() {
        return method.getParameterCount();
    }

    @Override
    public Uni<?> invoke(Object[] parameters) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object result = method.invoke(instance, parameters);
        if(result == null) {
            return Uni.createFrom().nullItem();
        } else if(result instanceof Uni) {
            return (Uni<?>)result;
        } else {
            return Uni.createFrom().item(result);
        }
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getHttpMethod() {
        return httpMethod;
    }
    
    @Override
    public Class<?> getControllerClass() {
        return instance.getClass();
    }

    @Override
    public MethodParameter getSpecialRequestParameter(Class<?> aClass) {
        return specialRequestParameters.get(aClass);
    }

    @Override
    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public Class<?>[] getGenericReturnTypes() {
        return genericReturnTypes;
    }
}
