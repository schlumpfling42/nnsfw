package net.nnwsf.controller.documentation.model;

public class CollectionClassDescription extends ClassDescription {

    public static CollectionClassDescription of(ClassDescription genericClassDescription) {
        return new CollectionClassDescription(genericClassDescription);
    }

    private ClassDescription genericClassDescription;

    public ClassDescription getGenericClassDescription() {
        return genericClassDescription;
    }

    public CollectionClassDescription(ClassDescription genericClassDescription) {
        this.genericClassDescription = genericClassDescription;
    }

    @Override
    public String asString() {
        return "[ " + genericClassDescription.asString() + " ]" ;
    }
}