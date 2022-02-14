package net.nnwsf.nocode;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using=JsonDeserializer.None.class)
public class SchemaPrimitive extends SchemaElement {
}
