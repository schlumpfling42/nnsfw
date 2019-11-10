package net.nnwsf.handler;

import java.lang.annotation.Annotation;

class MethodParameter {

    private final String name;
    private final Class<?> type;

    MethodParameter(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    @Override
    public String toString() {
        return "MethodParameter [name=" + name + ", type=" + type + "]";
    }

}
