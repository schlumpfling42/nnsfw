package net.nnwsf.nocode;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using=JsonDeserializer.None.class)
public class SchemaArray extends SchemaElement {
    private SchemaElement items;

    public SchemaElement getItems() {
        return items;
    }

    public void setItems(SchemaElement items) {
        this.items = items;
    }
}
