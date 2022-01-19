package net.nnwsf.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import net.nnwsf.persistence.PersistenceManager;
import net.nnwsf.service.ServiceManager;

public class InjectionHelper {
    private static InjectionHelper instance = new InjectionHelper();

    private final Map<String, Object> injectables = new HashMap<>();

    private InjectionHelper() {
    }

    public static <T> T getInjectable(Class<T> aClass, String name) throws Exception {
        return instance.internalGetInjectable(aClass, name);
    }

    @SuppressWarnings("unchecked")
    private synchronized <T> T internalGetInjectable(Class<T> aClass, String name) throws Exception {
        String classKey = aClass.getName() + (name == null ? "" : ":" + name);
        Object injectable = injectables.get(classKey);
        if(injectable == null) {
            Collection<Field> annotationFields = null;
            if(ServiceManager.isService(aClass)) {
                injectable = ServiceManager.createService(aClass, name);
            } else if(PersistenceManager.isRepository(aClass)) {
                injectable = PersistenceManager.createRepository(aClass);
            } else {
                Class<?> implementationClass = ClassDiscovery.getImplementation(aClass);
                injectable = implementationClass.getConstructor().newInstance();
            }
            injectables.put(classKey, injectable);
            
            if(injectable != null) {
                annotationFields = ReflectionHelper.findAnnotationFields(injectable.getClass(), Inject.class);
                for(Field field : annotationFields) {
                    Named named = field.getAnnotation(Named.class);
                    field.set(injectable, getInjectable(field.getType(), named == null ? null : named.value()));
                }
            }
        }
        return (T)injectable;
    }
}
