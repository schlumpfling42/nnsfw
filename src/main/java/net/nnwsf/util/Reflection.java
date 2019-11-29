package net.nnwsf.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.nnwsf.configuration.Server;
import net.nnwsf.configuration.ServerConfiguration;

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

    public Collection<Field> findAnnotationFields(Class<?> aClass, Class<?> annotationClass) {
        Collection<Field> annotatedFields = new ArrayList<>();
        Field[] fields = aClass.getDeclaredFields();
        for(Field field : fields) {
            Annotation[] annotations = field.getAnnotations();
            for(Annotation annotation : annotations) {
                    if (annotation.annotationType().isAssignableFrom(annotationClass)) {
                        field.setAccessible(true);
                        annotatedFields.add(field);
                    }
            }
        }
        return annotatedFields;
    }

    public <T extends Annotation> T findAnnotation(Class<?> aClass, Class<T> annotationClass) {
        if(Object.class.equals(aClass)) {
            return null;
        }
        T annotation = aClass.getAnnotation(annotationClass);
        if(annotation != null) {
            return annotation;
        }
        AnnotatedType[] annotatedTypes = aClass.getAnnotatedInterfaces();
        if(annotatedTypes != null) {
            for(AnnotatedType annotatedType : annotatedTypes) {
                annotation = (T) ((Class)annotatedType.getType()).getAnnotation(annotationClass);
                if(annotation != null) {
                    return annotation;
                }
            }
        }
        return findAnnotation(aClass.getSuperclass(), annotationClass);
    }

    public <T extends Annotation> T findAnnotation(Method method, Class<T> annotationClass) {
        T annotation = method.getAnnotation(annotationClass);
        if(annotation != null) {
            return annotation;
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