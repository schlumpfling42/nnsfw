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
        if("save".equals(method.getName()) && method.getParameterTypes().length == 1) {
            if(entityClass.isInstance(args[0])) {
                try(EntityManagerHolder entityManagerHolder = PersistenceManager.createEntityManager()) {
                    entityManagerHolder.beginTransaction();
                    Object result = entityManagerHolder.getEntityManager().merge(args[0]);
                    entityManagerHolder.commitTransaction();
                    return result;
                }
            } else {
                throw new IllegalAccessException("enity type doesn't match");
            }
        }
        if("delete".equals(method.getName()) && method.getParameterTypes().length == 1) {
            if(entityClass.isInstance(args[0])) {
                try(EntityManagerHolder entityManagerHolder = PersistenceManager.createEntityManager()) {
                    entityManagerHolder.beginTransaction();
                    entityManagerHolder.getEntityManager().remove(args[0]);
                    entityManagerHolder.commitTransaction();
                    return null;
                }
            } else {
                throw new IllegalAccessException("enity type doesn't match");
            }
        }
        if("findById".equals(method.getName()) && method.getParameterTypes().length == 1) {
            if(idClass.isInstance(args[0])) {
                try(EntityManagerHolder entityManagerHolder = PersistenceManager.createEntityManager()) {
                    entityManagerHolder.beginTransaction();
                    Object result = entityManagerHolder.getEntityManager().find(entityClass, args[0]);
                    entityManagerHolder.commitTransaction();
                    return result;
                }
            } else {
                throw new IllegalAccessException("id type doesn't match");
            }
        }
        if("findAll".equals(method.getName()) && method.getParameterTypes().length == 0) {
            try(EntityManagerHolder entityManagerHolder = PersistenceManager.createEntityManager()) {
                entityManagerHolder.beginTransaction();
                Object result = entityManagerHolder.getEntityManager().createQuery("select e from " + entityClass.getSimpleName() + " e").getResultList();
                entityManagerHolder.commitTransaction();
                return result;
            }
        }
        return null;
    }

}
