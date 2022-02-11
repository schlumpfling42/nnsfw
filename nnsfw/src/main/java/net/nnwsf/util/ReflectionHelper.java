package net.nnwsf.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReflectionHelper {

    private static Logger log = Logger.getLogger(ReflectionHelper.class.getName());

    public static Map<Annotation, Method> findAnnotationMethods(Class<?> aRootClass, Class<?>... annotationClasses) {
        Map<Annotation, Method> annotationMethodMap = new IdentityHashMap<>();
        Collection<Class<?>> classes = getAllClassesAndInterfaces(aRootClass);
        for(Class<?> aClass : classes){
            Method[] methods = aClass.getMethods();
            for(Method method : methods) {
                Annotation[] annotations = method.getAnnotations();
                for(Annotation annotation : annotations) {
                    for(Class<?> annotationClass : annotationClasses) {
                        if (annotation.annotationType().isAssignableFrom(annotationClass)) {
                            annotationMethodMap.put((Annotation)annotation, method);
                        }
                    }
                }
            }
        }
        return annotationMethodMap;
    }

    public static Collection<Field> findAnnotationFields(Class<?> aClass, Class<?> annotationClass) {
        Collection<Field> annotatedFields = new ArrayList<>();
        Collection<Class<?>> classes = getAllClassesAndInterfaces(aClass);
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
                annotation = (T) ((Class<?>)annotatedType.getType()).getAnnotation(annotationClass);
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
    public static <T> T getInstance(Class<T> aClass) {
        if(aClass != null) {
            try {
                return aClass.getConstructor().newInstance();
            } catch(Exception e) {
                log.log(Level.SEVERE, "Unable to create instance of " + aClass);
            }
        }
        return null;
    }

    public static String getValue(Annotation methodAnnotation, String value) {
        try {
            return (String)methodAnnotation.getClass().getMethod(value).invoke(methodAnnotation);
        } catch(Exception e) {
            throw new RuntimeException("Unable to get value from annotation " + methodAnnotation, e);
        }
    }

    public static Collection<Class<?>>  getAllClassesAndInterfaces(Class<?> aClass) {
		Collection<Class<?>> allClassesAndInterfaces = new HashSet<>();
		if(aClass != null && !Object.class.equals(aClass) && (Proxy.isProxyClass(aClass) || isContainedIn(aClass.getPackage()))) {
			allClassesAndInterfaces.add(aClass);
			for(Class<?> anInterface: aClass.getInterfaces()) {
				allClassesAndInterfaces.add(anInterface);
				allClassesAndInterfaces.addAll(getAllClassesAndInterfaces(anInterface.getSuperclass()));
			}
			allClassesAndInterfaces.addAll(getAllClassesAndInterfaces(aClass.getSuperclass()));
		}
		return allClassesAndInterfaces;
    }

    private static boolean isContainedIn(Package aPackage) {
        if(aPackage == null) {
            return false;
        }
        return !aPackage.getName().startsWith("java");
    }

    public static Map<Annotation, Parameter> findParameterAnnotations(Method method, Class<?>... annotationClasses) {
        Map<Annotation, Parameter> annotations = new LinkedHashMap<>();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Parameter[] parameters = method.getParameters();
		for (int i = 0; i < parameterAnnotations.length; i++) {
			if (parameterAnnotations[i] != null) {
                Parameter parameter = parameters[i];
                for (Annotation aParameterAnnotation : parameterAnnotations[i]) {
                    Arrays.stream(annotationClasses).forEach(annotationClass -> {
                        if (aParameterAnnotation.annotationType().isAssignableFrom(annotationClass)) {
                            annotations.put(aParameterAnnotation, parameter);
                        }
                    });
                }
			}
		}
		return annotations;
    }
    public static Parameter findParameter(Method method, Class<?> annotationClass) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Parameter[] parameters = method.getParameters();
		for (int i = 0; i < parameterAnnotations.length; i++) {
			if (parameterAnnotations[i] != null) {
                Parameter parameter = parameters[i];
                for (Annotation aParameterAnnotation : parameterAnnotations[i]) {
                        if (aParameterAnnotation.annotationType().isAssignableFrom(annotationClass)) {
                            return parameter;
                        }
                }
			}
		}
		return null;
    } 

    public static Class<?> getGenericType(Field aField) {
        Type type = aField.getGenericType();

        if (type instanceof ParameterizedType) {

            ParameterizedType pType = (ParameterizedType)type;
            Type[] types = pType.getActualTypeArguments();

            for (Type tp: types) {
                return (Class<?>)tp;
            }
        }
        return null;
    }

    public static Class<?>[] getGenericTypes(Field aField) {
        Type type = aField.getGenericType();

        if (type instanceof ParameterizedType) {

            ParameterizedType pType = (ParameterizedType)type;
            Type[] types = pType.getActualTypeArguments();

            Class<?>[] classes = new Class[types.length];
            for (int i=0; i<types.length; i++) {
                try {
                    classes[i] = (Class<?>)types[i];
                } catch(ClassCastException ce) {

                }
            }
            return classes;
        }
        return new Class[0];
    }

    public static Class<?>[] getGenericTypes(Method aMethod) {
        Type type = aMethod.getGenericReturnType();

        if (type instanceof ParameterizedType) {

            ParameterizedType pType = (ParameterizedType)type;
            Type[] types = pType.getActualTypeArguments();

            Class<?>[] classes = new Class[types.length];
            for (int i=0; i<types.length; i++) {
                try {
                    classes[i] = (Class<?>)types[i];
                } catch(ClassCastException ce) {

                }
            }
            return classes;
        }
        return new Class[0];
    }

    public static void copy(Object entity, Object object, Map<String, Field> fields) {
        fields.values().forEach(aField -> {
            try {
                aField.setAccessible(true);
                aField.set(entity, aField.get(object));
            } catch(Exception e) {
                log.log(Level.WARNING, "Unable to set value for field");
            }
        });
    }

    public static Map<String, Field> findFields(Class<?> entityClass) {
        if(Object.class.equals(entityClass)) {
            return new HashMap<>();
        } else {
            Map<String, Field> result = findFields(entityClass.getSuperclass());
            Arrays.stream(entityClass.getDeclaredFields()).forEach(aField -> {
                result.put(aField.getName(), aField);
            });
            return result;
        }
    }
}