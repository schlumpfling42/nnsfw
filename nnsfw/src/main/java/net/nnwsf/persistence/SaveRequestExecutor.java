package net.nnwsf.persistence;

import java.lang.reflect.Method;

public class SaveRequestExecutor extends Executor {

    SaveRequestExecutor(Class<?> entityClass, Class<?> idClass, Method method) {
        super(entityClass, idClass, method);
    }

    @Override
    public Object execute(EntityManagerHolder entityManagerHolder, Object[] params) {
        if(entityClass.isInstance(params[0])) {
            entityManagerHolder.beginTransaction();
            Object result = entityManagerHolder.getEntityManager().merge(params[0]);
            entityManagerHolder.commitTransaction();
            return result;
        } else {
            throw new IllegalArgumentException("Entity type doesn't match");
        }
    }
    
}
