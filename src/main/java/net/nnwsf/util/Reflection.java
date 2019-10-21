package net.nnwsf.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.nnwsf.configuration.ServerConfiguration;
import net.nnwsf.configuration.ServerConfigurationImpl;

public class Reflection {

    private static Logger log = Logger.getLogger(Reflection.class.getName());

    private static Reflection instance = new Reflection();

    public static Reflection getInstance() {
        return instance;
    }

    public ServerConfigurationImpl getConfiguration(Class<?> aClass) {
        ServerConfiguration annotation = findAnnotation(aClass, ServerConfiguration.class);
        if(annotation == null) {
            throw new RuntimeException("No configuration provided");
        }
        try {
            return annotation.value().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Invalid configuration", e);
        }
    }

    public <T extends Annotation> Map<T, Collection<Method>> findAnnotationMethods(Class<?> aClass, Class<T> annotationClass) {
        Map<T, Collection<Method>> annotationMethodMap = new HashMap<>();
        Method[] methods = aClass.getMethods();
        for(Method method : methods) {
            Annotation[] annotations = method.getAnnotations();
            for(Annotation annotation : annotations) {
                if(annotation.annotationType().isAssignableFrom(annotationClass)) {
                    Collection<Method> annotatedMethods = annotationMethodMap.get((T)annotation);
                    if(annotatedMethods == null) {
                        annotatedMethods = new HashSet<>();
                        annotationMethodMap.put((T)annotation, annotatedMethods);
                    }
                    annotatedMethods.add(method);
                }
            }
        }
        return annotationMethodMap;
    }

    public <T extends Annotation> T findAnnotation(Class<?> aClass, Class<T> annotationClass) {
        Annotation[] annotations = aClass.getAnnotations();
        for(Annotation annotation : annotations) {
            if(annotation.annotationType().isAssignableFrom(annotationClass)) {
                return (T)annotation;
            }
        }
        return null;
    }
}