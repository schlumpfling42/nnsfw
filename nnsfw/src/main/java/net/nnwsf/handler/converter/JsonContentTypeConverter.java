package net.nnwsf.handler.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

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
