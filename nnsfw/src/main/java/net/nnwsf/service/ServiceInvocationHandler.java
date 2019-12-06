package net.nnwsf.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.transaction.Transactional;

import net.nnwsf.persistence.EntityManagerHolder;
import net.nnwsf.persistence.PersistenceManager;
import net.nnwsf.util.ReflectionHelper;

public class ServiceInvocationHandler implements InvocationHandler {

    private final Object service;

    public ServiceInvocationHandler(Object aService) {
        this.service = aService;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(ReflectionHelper.findAnnotation(method, Transactional.class) != null) {
            try(EntityManagerHolder entityManager = PersistenceManager.createEntityManager()) {
                entityManager.beginTransaction();
                Object result = method.invoke(service, args);
                entityManager.commitTransaction();
                return result;
            }
        }
        return method.invoke(service, args);
    }

	public Object getServiceObject() {
		return service;
	}

}
