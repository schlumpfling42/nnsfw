package net.nnwsf.handler;

import java.lang.annotation.Annotation;

public class AnnotatedMethodParameter extends MethodParameter {

    private final Annotation annotation;

    public AnnotatedMethodParameter(Annotation annotation, String name, Class<?> type, int index) {
        super(name, type, index);
        this.annotation = annotation;
    }

    public Annotation getAnnotation() {
        return annotation;
    }
    @Override
    public String toString() {
        return "MethodParameter [annotation=" + annotation + ", name=" + getName() + ", type=" + getType() + "]";
    }

}
