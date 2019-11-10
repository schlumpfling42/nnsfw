package net.nnwsf.handler;

import java.lang.annotation.Annotation;

class AnnotatedMethodParameter extends MethodParameter {

    private final Annotation annotation;

    AnnotatedMethodParameter(Annotation annotation, String name, Class<?> type) {
        super(name, type);
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
