package net.nnwsf.persistence;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Query;

import net.nnwsf.resource.Page;
import net.nnwsf.resource.PageRequest;

public class FindWithPageRequestExecutor extends Executor {

    FindWithPageRequestExecutor(Class<?> entityClass, Class<?> idClass, Method method) {
        super(entityClass, idClass, method);
    }

    @Override
    public Object execute(EntityManagerHolder entityManagerHolder, Object[] params) {
        if(PageRequest.class.isInstance(params[0])) {
            PageRequest pageRequest = (PageRequest)params[0];
            int start = pageRequest.getPage() * pageRequest.getSize();
            entityManagerHolder.beginTransaction();
            long totalCount = (Long)entityManagerHolder.getEntityManager().createQuery("select Count(e) from " + entityClass.getSimpleName() + " e").getSingleResult();
            Query query = entityManagerHolder.getEntityManager().createQuery("select e from " + entityClass.getSimpleName() + " e");
            query.setFirstResult(start);
            query.setMaxResults(pageRequest.getSize());
            List<?> resultList = query.getResultList();
            entityManagerHolder.commitTransaction();
            return new Page<>(totalCount, pageRequest, resultList);
        } else {
            throw new IllegalArgumentException("Unable to execute db query because of invalid parameters: " + Arrays.stream(params).map(String::valueOf).collect(Collectors.joining(",")));
        }
    }
    
}
