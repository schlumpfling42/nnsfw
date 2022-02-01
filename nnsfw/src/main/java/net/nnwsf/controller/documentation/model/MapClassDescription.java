package net.nnwsf.controller.documentation.model;

public class MapClassDescription extends ClassDescription {

    public static MapClassDescription of(ClassDescription keyDescription, ClassDescription valueDescription) {
        return new MapClassDescription(keyDescription, valueDescription);
    }

    private ClassDescription keyDescription;
    private ClassDescription valueDescription;

    public MapClassDescription(ClassDescription keyDescription, ClassDescription valueDescription) {
        this.keyDescription = keyDescription;
        this.valueDescription = valueDescription;
    }

    public ClassDescription getKeyDescription() {
        return keyDescription;
    }

    public ClassDescription getValueDescription() {
        return valueDescription;
    }

    @Override
    public String asString() {
        return "{ " + keyDescription.asString() + ": " + valueDescription.asString() + " }" ;
    }
}