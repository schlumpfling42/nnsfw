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
            try(EntityManagerHolder entityManagerHolder = PersistenceManager.createEntityManager()) {
                entityManagerHolder.beginTransaction();
                Object result = entityManagerHolder.getEntityManager().merge(args[0]);
                entityManagerHolder.commitTransaction();
                return result;
            }
        }
        if("delete".equals(method.getName()) && method.getParameterTypes().length == 1) {
            try(EntityManagerHolder entityManagerHolder = PersistenceManager.createEntityManager()) {
                entityManagerHolder.beginTransaction();
                entityManagerHolder.getEntityManager().remove(args[0]);
                entityManagerHolder.commitTransaction();
                return null;
            }
        }
        if("findById".equals(method.getName()) && method.getParameterTypes().length == 1) {
            try(EntityManagerHolder entityManagerHolder = PersistenceManager.createEntityManager()) {
                entityManagerHolder.beginTransaction();
                Object result = entityManagerHolder.getEntityManager().find(entityClass, args[0]);
                entityManagerHolder.commitTransaction();
                return result;
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
