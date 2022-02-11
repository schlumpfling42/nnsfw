package net.nnwsf.controller.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import net.nnwsf.application.Constants;
import net.nnwsf.controller.converter.annotation.Converter;

@Converter(contentType = Constants.CONTENT_TYPE_APPLICATION_JSON)
public class JsonContentTypeConverter implements ContentTypeConverter{

    private final ObjectMapper mapper;
    public JsonContentTypeConverter() {
        this.mapper = new ObjectMapper();
        this.mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
    }

    @Override
    public void writeTo(Object output, OutputStream outputStream) throws IOException {
        mapper.writeValue(outputStream, output);
    }

    @Override
    public Object readFrom(InputStream inputStream, Class<?> type) throws IOException {
        return mapper.readValue(inputStream, type);
    }
    
}
