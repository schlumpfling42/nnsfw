package net.nnwsf.util;

import java.io.IOException;
import java.io.InputStream;

public class ResourceUtil {
    public static String getResourceAsString(Class<?> aClass, String resourceName) throws IOException{
        InputStream inputStream = null;
        try {
            inputStream = aClass.getResourceAsStream(resourceName);
            if(inputStream == null) {
                inputStream = aClass.getResourceAsStream("." +resourceName);
                if(inputStream == null) {
                    inputStream = aClass.getResourceAsStream("./" +resourceName);
                }
            }
            if(inputStream == null) {
                throw new RuntimeException("Unable to load resource: " + resourceName);
            }
            return new String(inputStream.readAllBytes(), "utf-8");
        } finally {
            if(inputStream != null) {
                inputStream.close();
            }
        }
    }
}
