package net.nnwsf.service;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import javax.transaction.Transactional;

import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.nnwsf.configuration.Default;
import net.nnwsf.persistence.EntityManagerHolder;
import net.nnwsf.persistence.PersistenceManager;
import net.nnwsf.util.ReflectionHelper;

public class ServiceInterceptor {
    @RuntimeType
    public static Object intercept(@Origin Method method,
            @SuperCall Callable<?> callable) throws Exception {
        if (ReflectionHelper.findAnnotation(method, Transactional.class) != null) {
            try (EntityManagerHolder entityManager = PersistenceManager.createEntityManager(Default.DATASOURCE_NAME)) {
                entityManager.beginTransaction();
                Object result = callable.call();
                ;
                entityManager.commitTransaction();
                return result;
            }
        }
        return callable.call();
    }
}
