package net.nnwsf.nocode;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class SchemeElementDeserializer extends JsonDeserializer<SchemaElement> {

    private final static ObjectMapper mapper;

    static
    {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    @Override
    public SchemaElement deserialize(JsonParser jp, DeserializationContext ctxt) throws 
            IOException, JsonProcessingException {
        ObjectCodec codec = jp.getCodec();
        JsonNode node = codec.readTree(jp);

        if (node.has("type")) {
            String type = node.findPath("type").asText(); {
                switch(type) {
                    case "object":
                        return codec.treeToValue(node, SchemaObject.class);
                    case "array":
                        return codec.treeToValue(node, SchemaArray.class);
                    case "string":
                    case "integer":
                    case "long":
                    case "short":
                    case "float":
                    case "double":
                    case "char":
                    case "byte":
                    case "boolean":
                    case "date":
                        return codec.treeToValue(node, SchemaPrimitive.class);
                    default:
                        throw new IllegalArgumentException();
                }
            }
        } else if (node.has("$ref")) {
            return codec.treeToValue(node, SchemaReference.class);
        }
        throw new IllegalArgumentException();     
    }
}