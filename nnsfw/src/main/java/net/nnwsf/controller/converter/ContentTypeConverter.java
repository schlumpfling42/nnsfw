package net.nnwsf.controller.converter;

import java.io.IOException;
import java.io.InputStream;

import io.vertx.mutiny.core.buffer.Buffer;

public interface ContentTypeConverter {
    Object readFrom(InputStream inputStream, Class<?> type) throws IOException;
    void writeTo(Object output, Buffer outputStream) throws IOException;
}
