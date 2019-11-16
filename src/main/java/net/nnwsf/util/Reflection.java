package net.nnwsf.util;

import net.nnwsf.configuration.Server;
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

    public ServerConfiguration getConfiguration(Class<?> aClass) {
        Server annotation = findAnnotation(aClass, Server.class);
        if(annotation == null) {
            throw new RuntimeException("No configuration provided");
        }
        try {
            return annotation.value().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Invalid configuration", e);
        }
    }

    public Map<Annotation, Collection<Method>> findAnnotationMethods(Class<?> aClass, Class<?>... annotationClasses) {
        Map<Annotation, Collection<Method>> annotationMethodMap = new HashMap<>();
        Method[] methods = aClass.getMethods();
        for(Method method : methods) {
            Annotation[] annotations = method.getAnnotations();
            for(Annotation annotation : annotations) {
                for(Class<?> annotationClass : annotationClasses) {
                    if (annotation.annotationType().isAssignableFrom(annotationClass)) {
                        Collection<Method> annotatedMethods = annotationMethodMap.get(annotation);
                        if (annotatedMethods == null) {
                            annotatedMethods = new HashSet<>();
                            annotationMethodMap.put((Annotation)annotation, annotatedMethods);
                        }
                        annotatedMethods.add(method);
                    }
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

    public <T extends Annotation> Collection<T> findAnnotations(Class<?> aClass, Class<T> annotationClass) {
        Collection<T> foundAnnotations = new ArrayList<>();
        Annotation[] annotations = aClass.getAnnotations();
        for(Annotation annotation : annotations) {
            if(annotation.annotationType().isAssignableFrom(annotationClass)) {
                foundAnnotations.add((T)annotation);
            }
        }
        return foundAnnotations;
    }

    public <T> Collection<T> getInstances(Collection<Class<T>> classes) {
        Collection<T> instances = new ArrayList<>();
        if(classes != null) {
            for (Class<T> aClass : classes) {
                try {
                    instances.add(aClass.newInstance());
                } catch(Exception e) {
                    log.log(Level.SEVERE, "Unable to create instance of " + aClass);
                }
            }
        }
        return instances;
    }

    public String getValue(Annotation methodAnnotation, String value) {
        try {
            return (String)methodAnnotation.getClass().getMethod(value, new Class[0]).invoke(methodAnnotation, new Object[0]);
        } catch(Exception e) {
            throw new RuntimeException("Unable to get value from annotation " + methodAnnotation, e);
        }
    }
}