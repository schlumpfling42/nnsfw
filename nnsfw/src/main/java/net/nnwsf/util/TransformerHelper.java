package net.nnwsf.util;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.nnwsf.persistence.Property;

public class TransformerHelper {

    @SuppressWarnings("all")
    private final class PropertyImplementation implements Property {
        private final Entry<String, String> configurationEntry;
        private PropertyImplementation(Entry<String, String> entry) {
            configurationEntry = entry;
        }
		@Override
		public Class<? extends Annotation> annotationType() {
		    return Property.class;
		}

		@Override
		public String value() {
		    return configurationEntry.getValue();
		}

		@Override
		public String name() {
		    return configurationEntry.getKey();
		}
	}

	@FunctionalInterface
    public interface Transformer<T> {
         T transform(Object from);
    }

    public static <T, F> T transform(F from, Class<T> toClass) {
        return instance.internalTransform(from, toClass);
    }

    private static TransformerHelper instance = new TransformerHelper();

    private final Map<Class<?>, Transformer<?>> transformers = new HashMap<>();

    @SuppressWarnings("unchecked")
    private TransformerHelper() {
        transformers.put(Class.class, value -> {
            if(value == null) {
                return null;
            }
            try {
                return Class.forName(value.toString());
            } catch(Exception e) {
                throw new RuntimeException("Unable to determine class for " + value);
            }
        });
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
        transformers.put(Property[].class, value -> {
            if(value != null) {
                if(value instanceof Map) {
                    final Map<String, String> aMap = (Map<String, String>)value;
                    Property[] properties = aMap.entrySet().stream().map(e -> new PropertyImplementation(e)).collect(Collectors.toList()).toArray(new Property[0]);
                    return properties;
                }
                
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
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