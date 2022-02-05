package net.nnwsf.persistence;

import java.lang.reflect.Method;

public class FindByIdRequestExecutor extends Executor {

    FindByIdRequestExecutor(Class<?> entityClass, Class<?> idClass, Method method) {
        super(entityClass, idClass, method);
    }

    @Override
    public Object execute(EntityManagerHolder entityManagerHolder, Object[] params) {
        if(idClass.isInstance(params[0])) {
            entityManagerHolder.beginTransaction();
            Object result = entityManagerHolder.getEntityManager().find(entityClass, params[0]);
            entityManagerHolder.commitTransaction();
            return result;
        } else {
            throw new IllegalArgumentException("id type doesn't match");
        }
    }
    
}
