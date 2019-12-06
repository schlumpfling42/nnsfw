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

public class ReflectionHelper {

    private static Logger log = Logger.getLogger(ReflectionHelper.class.getName());

    public static Map<Annotation, Collection<Method>> findAnnotationMethods(Class<?> aClass, Class<?>... annotationClasses) {
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

    public static Collection<Field> findAnnotationFields(Class<?> aClass, Class<?> annotationClass) {
        Collection<Field> annotatedFields = new ArrayList<>();
        Collection<Class<?>> classes = ReflectionHelper.getAllClassesAndInterfaces(aClass, ClassDiscovery.getPackagesToScan());
        for(Class<?> aClassToCheck : classes) {
            Field[] fields = aClassToCheck.getDeclaredFields();
            for(Field field : fields) {
                Annotation[] annotations = field.getAnnotations();
                for(Annotation annotation : annotations) {
                        if (annotation.annotationType().isAssignableFrom(annotationClass)) {
                            field.setAccessible(true);
                            annotatedFields.add(field);
                        }
                }
            }
        }
        return annotatedFields;
    }

    public static <T extends Annotation> T findAnnotation(Class<?> aClass, Class<T> annotationClass) {
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

    public static <T extends Annotation> T findAnnotation(Method method, Class<T> annotationClass) {
        T annotation = method.getAnnotation(annotationClass);
        if(annotation != null) {
            return annotation;
        }
        return null;
    }

    public static <T extends Annotation> Collection<T> findAnnotations(Class<?> aClass, Class<T> annotationClass) {
        Collection<T> foundAnnotations = new ArrayList<>();
        Annotation[] annotations = aClass.getAnnotations();
        for(Annotation annotation : annotations) {
            if(annotation.annotationType().isAssignableFrom(annotationClass)) {
                foundAnnotations.add(annotationClass.cast(annotation));
            }
        }
        return foundAnnotations;
    }

    public static <T> Collection<T> getInstances(Collection<Class<T>> classes) {
        Collection<T> instances = new ArrayList<>();
        if(classes != null) {
            for (Class<T> aClass : classes) {
                try {
                    instances.add(aClass.getConstructor().newInstance());
                } catch(Exception e) {
                    log.log(Level.SEVERE, "Unable to create instance of " + aClass);
                }
            }
        }
        return instances;
    }

    public static String getValue(Annotation methodAnnotation, String value) {
        try {
            return (String)methodAnnotation.getClass().getMethod(value).invoke(methodAnnotation);
        } catch(Exception e) {
            throw new RuntimeException("Unable to get value from annotation " + methodAnnotation, e);
        }
    }
	public static Collection<Class<?>>  getAllClassesAndInterfaces(Class<?> aClass, Collection<Package> packagesToScan) {
		Collection<Class<?>> allClassesAndInterfaces = new HashSet<>();
		if(aClass != null && !Object.class.equals(aClass) && isContainedIn(packagesToScan, aClass.getPackage())) {
			allClassesAndInterfaces.add(aClass);
			for(Class<?> anInterface: aClass.getInterfaces()) {
				allClassesAndInterfaces.add(anInterface);
				allClassesAndInterfaces.addAll(getAllClassesAndInterfaces(anInterface.getSuperclass(), packagesToScan));
			}
			allClassesAndInterfaces.addAll(getAllClassesAndInterfaces(aClass.getSuperclass(), packagesToScan));
		}
		return allClassesAndInterfaces;
    }
    
    private static boolean isContainedIn(Collection<Package> packagesToScan, Package aPackage) {
        return packagesToScan.stream().map(p -> p.getName()).anyMatch(name -> aPackage.getName().contains(name));
    } 
    
}