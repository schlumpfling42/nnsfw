package net.nnwsf.handler;

import io.undertow.util.AttachmentKey;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ControllerProxy {

    public static final AttachmentKey<ControllerProxy> CONTROLLER_PROXY_ATTACHMENT_KEY = AttachmentKey.create(ControllerProxy.class);

    private final Collection<Annotation> annotations;
    private final Method method;
    private final Object instance;
    private final AnnotatedMethodParameter[] annotatedMethodParameters;
    private final MethodParameter[] specialMethodParameters;
    private final List<String> pathElements;

    ControllerProxy(Object instance, Collection<Annotation> annotations, Method method, AnnotatedMethodParameter[] annotatedMethodParameters, MethodParameter[] specialMethodParameters, List<String> pathElements) {
        this.instance = instance;
        this.method = method;
        this.annotatedMethodParameters = annotatedMethodParameters;
        this.specialMethodParameters = specialMethodParameters;
        this.pathElements = pathElements;
        this.annotations = annotations;
    }

    public Collection<Annotation> getAnnotations() {
        return annotations;
    }

    public Method getMethod() {
        return method;
    }

    public Object getInstance() {
        return instance;
    }

    public AnnotatedMethodParameter[] getAnnotatedMethodParameters() {
        return annotatedMethodParameters;
    }

    public MethodParameter[] getSpecialMethodParameters() {
        return specialMethodParameters;
    }

    public List<String> getPathElements() {
        return pathElements;
    }

    @Override
    public String toString() {
        return "ControllerProxy [class=" + instance.getClass() + ", method=" + method + ", parameterNames="
                + Arrays.toString(annotatedMethodParameters) + "]";
    }
}
