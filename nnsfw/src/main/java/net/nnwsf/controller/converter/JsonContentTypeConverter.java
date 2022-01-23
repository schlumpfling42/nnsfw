package net.nnwsf.controller.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.nnwsf.controller.converter.annotation.Converter;

@Converter(contentType = "application/json")
public class JsonContentTypeConverter implements ContentTypeConverter{

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void writeTo(Object output, OutputStream outputStream) throws IOException {
        mapper.writeValue(outputStream, output);
    }

    @Override
    public Object readFrom(InputStream inputStream, Class<?> type) throws IOException {
        return mapper.readValue(inputStream, type);
    }
    
}
