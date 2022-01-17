package net.nnwsf.util;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.nnwsf.converter.TypeConverter;
import net.nnwsf.converter.annotation.Converter;

public class TypeUtil {

    static class ClassPair {

        public static ClassPair of(Converter converter) {
            ClassPair newClassPair = new ClassPair();
            newClassPair.source = converter.sourceType();
            newClassPair.target = converter.targetType();
            return newClassPair;
        }

        public static ClassPair of(Class<?> source, Class<?> target) {
            ClassPair newClassPair = new ClassPair();
            newClassPair.source = source;
            newClassPair.target = target;
            return newClassPair;
        }

        private Class<?> source;
        private Class<?> target;

        @Override
        public int hashCode() {
            return source.hashCode() + target.hashCode();
        }
        
        @Override
        public boolean equals(Object other) {
            if(other instanceof ClassPair) {
                ClassPair otherClassPair = (ClassPair)other;
                return this.source.equals(otherClassPair.source) && this.target.equals(otherClassPair.target);
            }
            return false;
        }
    }

    private static Logger log = Logger.getLogger(TypeUtil.class.getName());
    
    private static TypeUtil instance;

    public static void init() {
        if(instance == null) {
            try {
                var serviceClasses = ClassDiscovery.discoverAnnotatedClasses(TypeConverter.class, Converter.class);
                instance = new TypeUtil();
                serviceClasses.entrySet().forEach(entry -> {
                    try {
                        var converter = entry.getValue().getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
                        instance.converterMap.put(ClassPair.of(entry.getKey()), converter);
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to create type converters", e);
                    }
                });
            } catch (Exception e) {
                log.log(Level.SEVERE, "Unable to discover type converters", e);
                throw new RuntimeException("Unable to discover type converters", e);
            }
        }
    }

    public static <T> T toType(String value, Class<T> aClass) {
        if(instance != null) {
            return instance.internalToType(value, aClass);
        } else {
            log.log(Level.SEVERE, "TypeUtil not initialized");
            throw new RuntimeException("TypeUtil not initialized");
        }
   }

   private final Map<ClassPair, TypeConverter<?, ?>> converterMap = new HashMap<>();

   @SuppressWarnings("unchecked")
   private <T> T internalToType(String value, Class<T> aClass) {
    if(value != null) {
       var converter = (TypeConverter<String, T>)converterMap.get(ClassPair.of(String.class, aClass));
       if(converter != null) {
           return converter.convert(value);
       }
       throw new RuntimeException(String.format("Unable to convert %s from String to %s", value, aClass.getSimpleName()));
    }
    return null;
}
}
