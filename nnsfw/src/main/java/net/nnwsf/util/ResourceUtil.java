package net.nnwsf.util;

import java.io.IOException;
import java.io.InputStream;

public class ResourceUtil {
    public static String getResourceAsString(Class<?> aClass, String resourceName) throws IOException{
        try(InputStream inputStream = aClass.getClassLoader().getResourceAsStream(resourceName)) {
            return new String(inputStream.readAllBytes(), "utf-8");
        }
    }
    
}
