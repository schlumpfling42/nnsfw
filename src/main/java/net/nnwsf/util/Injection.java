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
            Object actualObject = null;
            Collection<Field> annotationFields = null;
            if(ServiceManager.getInstance().isService(aClass)) {
                injectable = ServiceManager.getInstance().createService(aClass, name);
                actualObject = ServiceManager.getInstance().getActualServiceObject(injectable);
            } else if(PersistenceManager.isRepository(aClass)) {
                injectable = PersistenceManager.createRepository(aClass);
            } else {
                Class<?> implementationClass = ClassDiscovery.getInstance().getImplementation(aClass);
                injectable = implementationClass.newInstance();
                actualObject = injectable;
            }
            injectables.put(aClass.getName(), injectable);
            
            if(actualObject != null) {
                annotationFields = Reflection.getInstance().findAnnotationFields(actualObject.getClass(), Inject.class);
                for(Field field : annotationFields) {
                    Named named = field.getAnnotation(Named.class);
                    field.set(actualObject, getInjectable(field.getType(), named == null ? null : named.value()));
                }
            }
        }
        return injectable;
    }
}
