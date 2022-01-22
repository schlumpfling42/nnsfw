package net.nnwsf.handler.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class TextContentTypeConverter implements ContentTypeConverter{

    @Override
    public void writeTo(Object output, OutputStream outputStream) throws IOException {
        outputStream.write(((String)output).getBytes("utf-8"));
    }

    @Override
    public Object readFrom(InputStream inputStream, Class<?> type) throws IOException {
        StringBuilder body = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(inputStream, "utf-8")) {
            char[] buffer = new char[256];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                body.append(buffer, 0, read);
            }
            return body.toString();
        }
    }
    
}
