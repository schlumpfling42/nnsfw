package net.nnwsf.nocode;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type", visible=true)
@JsonSubTypes( {
    @JsonSubTypes.Type(value=SchemaObject.class, name="object"),
    @JsonSubTypes.Type(value=SchemaPrimitive.class, name="string"),
    @JsonSubTypes.Type(value=SchemaPrimitive.class, name="integer"),
    @JsonSubTypes.Type(value=SchemaPrimitive.class, name="long"),
    @JsonSubTypes.Type(value=SchemaPrimitive.class, name="short"),
    @JsonSubTypes.Type(value=SchemaPrimitive.class, name="float"),
    @JsonSubTypes.Type(value=SchemaPrimitive.class, name="double"),
    @JsonSubTypes.Type(value=SchemaPrimitive.class, name="char"),
    @JsonSubTypes.Type(value=SchemaPrimitive.class, name="byte"),
    @JsonSubTypes.Type(value=SchemaPrimitive.class, name="boolean"),
    @JsonSubTypes.Type(value=SchemaPrimitive.class, name="date"),
    @JsonSubTypes.Type(value=SchemaArray.class, name="array")
})
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
