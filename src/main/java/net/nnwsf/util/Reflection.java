package net.nnwsf.util;

import net.nnwsf.configuration.ServerConfiguration;
import net.nnwsf.configuration.ServerConfigurationImpl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public <T> Collection<T> getInstances(Collection<Class<T>> classes) {
        Collection<T> instances = new ArrayList<>();
        for (Class<T> aClass : classes) {
            try {
                instances.add(aClass.newInstance());
            } catch(Exception e) {
                log.log(Level.SEVERE, "Unable to create instance of " + aClass);
            }
        }
        return instances;
    }
}