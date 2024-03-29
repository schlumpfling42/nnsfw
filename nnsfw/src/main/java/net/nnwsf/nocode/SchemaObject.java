package net.nnwsf.nocode;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using=JsonDeserializer.None.class)
public class SchemaObject extends SchemaElement {
    private Map<String, SchemaElement> properties;
    
    public Map<String, SchemaElement> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, SchemaElement> properties) {
        this.properties = properties;
    }
}
