package net.nnwsf.persistence;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import net.nnwsf.resource.PageRequest;

public class FindAllRequestExecutor extends Executor {

    FindAllRequestExecutor(Class<?> entityClass, Class<?> idClass, Method method) {
        super(entityClass, idClass, method);
    }

    @Override
    public Object execute(EntityManagerHolder entityManagerHolder, Object[] params) {
        if(PageRequest.class.isInstance(params[0])) {
            entityManagerHolder.beginTransaction();
            Object result = entityManagerHolder.getEntityManager().createQuery("select e from " + entityClass.getSimpleName() + " e").getResultList();
            entityManagerHolder.commitTransaction();
            return result;
        } else {
            throw new RuntimeException("Unable to execute db query because of invalid parameters: " + Arrays.stream(params).map(String::valueOf).collect(Collectors.joining(",")));
        }
    }
    
}
