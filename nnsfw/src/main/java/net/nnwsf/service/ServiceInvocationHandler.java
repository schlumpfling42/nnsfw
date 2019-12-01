package net.nnwsf.service;

import java.lang.reflect.Method;

import javax.transaction.Transactional;

import net.nnwsf.persistence.EntityManagerHolder;
import net.nnwsf.persistence.PersistenceManager;
import net.nnwsf.util.Reflection;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class ServiceInvocationHandler implements MethodInterceptor {

    public ServiceInvocationHandler() {
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        if(Reflection.getInstance().findAnnotation(method, Transactional.class) != null) {
            try(EntityManagerHolder entityManager = PersistenceManager.createEntityManager()) {
                entityManager.beginTransaction();
                Object result = proxy.invokeSuper(obj, args);;
                entityManager.commitTransaction();
                return result;
            }
        }
        return proxy.invokeSuper(obj, args);
    }

}
