package net.nnwsf.nocode;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using=SchemeElementDeserializer.class)
public abstract class SchemaElement {
    private String type;
    private String title;
    private String description;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [description=" + description + ", type=" + type + "]";
    }
}
