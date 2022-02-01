package net.nnwsf.controller.documentation.model;

import java.util.Map;
import java.util.stream.Collectors;

public class BeanClassDescription extends ClassDescription {

    public static BeanClassDescription of(Map<String, ClassDescription> attributeDescriptions) {
        return new BeanClassDescription(attributeDescriptions);
    }

    private Map<String, ClassDescription> attributeDescriptions;

    public BeanClassDescription(Map<String, ClassDescription> attributeDescriptions) {
        this.attributeDescriptions = attributeDescriptions;
    }

    public Map<String, ClassDescription> getAttributeDescriptions() {
        return attributeDescriptions;
    }

    @Override
    public String asString() {
        return "{ \n\t\"" + attributeDescriptions.entrySet().stream().map(entry -> entry.getKey() + "\": " + entry.getValue().asString()).collect(Collectors.joining("\n")) + "\n\t}" ;
    }
}