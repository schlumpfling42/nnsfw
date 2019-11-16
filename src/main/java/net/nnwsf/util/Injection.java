package net.nnwsf.util;

import net.nnwsf.controller.Controller;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Injection {
    private static Injection instance;

    public static Injection getInstance() {
        if(instance == null) {
            try {
                Collection<Class<Object>> serviceClasses = ClassDiscovery.getInstance().discoverAnnotatedClasses(Object.class, Controller.class);
                instance = new Injection(serviceClasses);
            } catch (Exception e) {
                throw new RuntimeException("Unable to discover services", e);
            }
        }
        return instance;
    }

    private final Collection<Class<Object>> serviceClasses;
    private final Map<String, Object> injectables = new HashMap<>();

    private Injection(Collection<Class<Object>> serviceClasses) {
        this.serviceClasses = serviceClasses;
    }

    public final synchronized Object getInjectable(Class<?> aClass) throws Exception {
        Object injectable = injectables.get(aClass.getName());
        if(injectable == null) {
            Class<?> implementationClass = ClassDiscovery.getInstance().getImplementation(aClass);
            injectable = implementationClass.newInstance();
            injectables.put(aClass.getName(), injectable);
            Collection<Field> annotationFields = Reflection.getInstance().findAnnotationFields(implementationClass, Inject.class);
            for(Field field : annotationFields) {
                field.set(injectable, getInjectable(field.getType()));
            }
        }
        return injectable;
    }
}
