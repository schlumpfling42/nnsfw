package net.nnwsf.handler;

import io.undertow.util.AttachmentKey;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

public interface EndpointProxy {

    AttachmentKey<EndpointProxy> ENDPOINT_PROXY_ATTACHMENT_KEY = AttachmentKey.create(EndpointProxy.class);

    Collection<Annotation> getAnnotations();

    String getContentType();

    int getParametersCount();

    AnnotatedMethodParameter[] getParameters();

    List<String> getPathElements();

    URLMatcher getUrlMatcher();

    Class<?> getControllerClass();

    String getPath();

    String getHttpMethod();

    MethodParameter getSpecialRequestParameter(Class<?> aClass);

    Class<?> getReturnType();

    Class<?>[] getGenericReturnTypes();

    Object invoke(Object[] parameters)  throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

}
