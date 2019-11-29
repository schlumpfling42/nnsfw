package net.nnwsf.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import net.nnwsf.persistence.Persistence;
import net.nnwsf.util.Reflection;

public class ServiceInvocationHandler implements InvocationHandler {

    private final Object service;
    private final ThreadLocal<EntityManager> entityManagerThreadLocal;

    public ServiceInvocationHandler(Object aService) {
        this.service = aService;
        entityManagerThreadLocal = new ThreadLocal<>();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        boolean entityManagerCreated = false;
        EntityManager entityManager = null;
        if(Reflection.getInstance().findAnnotation(method, Transactional.class) != null) {
            entityManager = entityManagerThreadLocal.get();
            if(entityManager == null) {
                entityManager = Persistence.createEntityManager();
                entityManagerThreadLocal.set(entityManager);
                if(entityManager != null) {
                    entityManagerCreated = true;
                }
            }
        };
        try {
            if(entityManagerCreated && entityManager != null) {
                entityManager.getTransaction().begin();
            }
            Object result = method.invoke(service, args);
            if(entityManagerCreated && entityManager != null) {
                entityManager.getTransaction().commit();
            }
            return result;
        } finally {
            if(entityManagerCreated) {
                if(entityManager != null && entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                    entityManager.close();
                }
            }
        }
    }

}
