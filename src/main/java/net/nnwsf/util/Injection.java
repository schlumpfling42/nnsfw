package net.nnwsf.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import net.nnwsf.service.Services;

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

    public final synchronized Object getInjectable(Class<?> aClass) throws Exception {
        Object injectable = injectables.get(aClass.getName());
        if(injectable == null) {
            if(Services.getInstance().isService(aClass)) {
                injectable = Services.getInstance().createService(aClass);
            } else {
                Class<?> implementationClass = ClassDiscovery.getInstance().getImplementation(aClass);
                injectable = implementationClass.newInstance();
            }
            injectables.put(aClass.getName(), injectable);
            Collection<Field> annotationFields = Reflection.getInstance().findAnnotationFields(injectable.getClass(), Inject.class);
            for(Field field : annotationFields) {
                field.set(injectable, getInjectable(field.getType()));
            }
        }
        return injectable;
    }
}
