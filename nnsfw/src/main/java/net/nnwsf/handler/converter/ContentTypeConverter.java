package net.nnwsf.handler.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ContentTypeConverter {
    Object readFrom(InputStream inputStream, Class<?> type) throws IOException;
    void writeTo(Object output, OutputStream outputStream) throws IOException;
}
