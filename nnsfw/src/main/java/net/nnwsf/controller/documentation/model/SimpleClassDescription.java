package net.nnwsf.controller.documentation.model;

public class SimpleClassDescription extends ClassDescription {

    public static SimpleClassDescription of(Class<?> aClass) {
        return new SimpleClassDescription(aClass.getSimpleName());
    }

    private String name;

    public SimpleClassDescription(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return name;
    }
}