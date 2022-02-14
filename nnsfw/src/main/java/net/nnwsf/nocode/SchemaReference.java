package net.nnwsf.nocode;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using=JsonDeserializer.None.class)
public class SchemaReference extends SchemaElement {

    @JsonAlias("$ref")
    private String reference;
    
    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
