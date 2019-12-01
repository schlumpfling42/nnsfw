package net.nnwsf.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import net.nnwsf.persistence.PersistenceManager;
import net.nnwsf.service.ServiceManager;

public class Injection {
    private static Injection instance;

    public static Injection getInstance() {
        if(instance == null) {
           instance = new Injection();
        }
        return instance;
    }

    private final Map<String, Object> injectables = new HashMap<>();

    private Injection() {
    }

    public final synchronized Object getInjectable(Class<?> aClass, String name) throws Exception {
        Object injectable = injectables.get(aClass.getName());
        if(injectable == null) {
            Collection<Field> annotationFields = null;
            if(ServiceManager.isService(aClass)) {
                injectable = ServiceManager.createService(aClass, name);
            } else if(PersistenceManager.isRepository(aClass)) {
                injectable = PersistenceManager.createRepository(aClass);
            } else {
                Class<?> implementationClass = ClassDiscovery.getImplementation(aClass);
                injectable = implementationClass.newInstance();
            }
            injectables.put(aClass.getName(), injectable);
            
            if(injectable != null) {
                annotationFields = Reflection.getInstance().findAnnotationFields(injectable.getClass(), Inject.class);
                for(Field field : annotationFields) {
                    Named named = field.getAnnotation(Named.class);
                    field.set(injectable, getInjectable(field.getType(), named == null ? null : named.value()));
                }
            }
        }
        return injectable;
    }
}
