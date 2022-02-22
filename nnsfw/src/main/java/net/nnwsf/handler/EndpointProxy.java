package net.nnwsf.handler;

import io.smallrye.mutiny.Uni;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public interface EndpointProxy {

    Collection<Annotation> getAnnotations();

    String getContentType();

    int getParametersCount();

    AnnotatedMethodParameter[] getParameters();

    Class<?> getControllerClass();

    String getPath();

    String getHttpMethod();

    MethodParameter getSpecialRequestParameter(Class<?> aClass);

    Class<?> getReturnType();

    Class<?>[] getGenericReturnTypes();

    Uni<?> invoke(Object[] parameters)  throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

}
