package net.nnwsf.persistence;

import java.lang.reflect.Method;

public class DeleteRequestExecutor extends Executor {

    DeleteRequestExecutor(Class<?> entityClass, Class<?> idClass, Method method) {
        super(entityClass, idClass, method);
    }

    @Override
    public Object execute(EntityManagerHolder entityManagerHolder, Object[] params) {
        if(entityClass.isInstance(params[0])) {
            entityManagerHolder.beginTransaction();
            Object mergedObject = entityManagerHolder.getEntityManager().merge(params[0]);
            entityManagerHolder.getEntityManager().remove(mergedObject);
            entityManagerHolder.commitTransaction();
            return null;
        } else {
            throw new IllegalArgumentException("Entity type doesn't match");
        }
    }
    
}
