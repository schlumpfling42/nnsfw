package net.nnwsf.util;

import java.util.HashMap;
import java.util.Map;

public class TransformerHelper {

    @FunctionalInterface
    public interface Transformer<T> {
         T transform(Object from);
    }

    public static <T, F> T transform(F from, Class<T> toClass) {
        return instance.internalTransform(from, toClass);
    }

    private static TransformerHelper instance = new TransformerHelper();

    private final Map<Class<?>, Transformer<?>> transformers = new HashMap<>();

    private TransformerHelper() {
        transformers.put(String.class, value -> String.valueOf(value));
        transformers.put(int.class, value -> {
            if(value == null) {
                return null;
            } else if(value instanceof Number) {
                return ((Number)value).intValue();
            } else {
                return Integer.valueOf(value.toString());
            }
        });
        transformers.put(Integer.class, value -> {
            if(value == null) {
                return null;
            } else if(value instanceof Number) {
                return ((Number)value).intValue();
            } else {
                return Integer.valueOf(value.toString());
            }
        });
        transformers.put(long.class, value -> {
            if(value == null) {
                return null;
            } else if(value instanceof Number) {
                return ((Number)value).longValue();
            } else {
                return Long.valueOf(value.toString());
            }
        });
        transformers.put(Long.class, value -> {
            if(value == null) {
                return null;
            } else if(value instanceof Number) {
                return ((Number)value).longValue();
            } else {
                return Long.valueOf(value.toString());
            }
        });
        transformers.put(float.class, value -> {
            if(value == null) {
                return null;
            } else if(value instanceof Number) {
                return ((Number)value).floatValue();
            } else {
                return Float.valueOf(value.toString());
            }
        });
        transformers.put(Float.class, value -> {
            if(value == null) {
                return null;
            } else if(value instanceof Number) {
                return ((Number)value).floatValue();
            } else {
                return Float.valueOf(value.toString());
            }
        });
        transformers.put(double.class, value -> {
            if(value == null) {
                return null;
            } else if(value instanceof Number) {
                return ((Number)value).doubleValue();
            } else {
                return Double.valueOf(value.toString());
            }
        });
        transformers.put(Double.class, value -> {
            if(value == null) {
                return null;
            } else if(value instanceof Number) {
                return ((Number)value).doubleValue();
            } else {
                return Double.valueOf(value.toString());
            }
        });
    }

    private  <T, F> T internalTransform(F from, Class<T> toClass) {
        if(from != null) {
            if(from.getClass().isAssignableFrom(toClass)) {
                return toClass.cast(from);
            }
            Transformer<T>  transformer = (Transformer<T>)transformers.get(toClass);
            if(transformer != null) {
                return transformer.transform(from);
            }
        }
        return toClass.cast(from);
    }

}