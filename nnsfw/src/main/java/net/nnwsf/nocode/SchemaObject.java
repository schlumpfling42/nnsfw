package net.nnwsf.nocode;

import java.util.Map;

public class SchemaObject extends SchemaElement {
    private Map<String, SchemaElement> properties;
    
    public Map<String, SchemaElement> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, SchemaElement> properties) {
        this.properties = properties;
    }
}
