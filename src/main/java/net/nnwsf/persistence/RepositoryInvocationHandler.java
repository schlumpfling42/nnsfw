package net.nnwsf.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;

import javax.persistence.Id;

import net.nnwsf.util.Reflection;

public class RepositoryInvocationHandler implements InvocationHandler {

    private final Class<?> entityClass;
    private final Class<?> idClass;

    public RepositoryInvocationHandler(Class<?> entityClass) {
        this.entityClass = entityClass;
        Collection<Field> idFields = Reflection.getInstance().findAnnotationFields(entityClass, Id.class);
        idClass = idFields.iterator().next().getType();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if("save".equals(method.getName()) && method.getParameterTypes().length == 1 && entityClass.equals(method.getParameterTypes()[0])) {
            try(EntityManagerHolder entityManagerHolder = PersistenceManager.createEntityManager()) {
                entityManagerHolder.beginTransaction();
                Object result = entityManagerHolder.getEntityManager().merge(args[0]);
                entityManagerHolder.commitTransaction();
                return result;
            }
        }
        if("findById".equals(method.getName()) && method.getParameterTypes().length == 1 && idClass.equals(method.getParameterTypes()[0])) {
            try(EntityManagerHolder entityManagerHolder = PersistenceManager.createEntityManager()) {
                entityManagerHolder.beginTransaction();
                Object result = entityManagerHolder.getEntityManager().find(entityClass, args[0]);
                entityManagerHolder.commitTransaction();
                return result;
            }
        }
        return null;
    }

}
