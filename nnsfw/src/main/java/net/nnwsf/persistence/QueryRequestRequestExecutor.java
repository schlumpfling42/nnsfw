package net.nnwsf.persistence;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

import javax.persistence.Query;

import net.nnwsf.persistence.annotation.QueryParameter;

public class QueryRequestRequestExecutor extends Executor {

    private final QueryParameter[] queryParameters;
    private final String queryString;

    QueryRequestRequestExecutor(Class<?> entityClass, Class<?> idClass, Method method) {
        super(entityClass, idClass, method);
        queryString = method.getAnnotation(net.nnwsf.persistence.annotation.Query.class).value();
        queryParameters = new QueryParameter[method.getParameterCount()];
        Annotation[][] parametersAnnotations = method.getParameterAnnotations();
        for(int i=0; i< method.getParameterCount(); i++) {
            Annotation[] queryParameterAnnotations = parametersAnnotations[i];
            if(queryParameterAnnotations != null) {
                for(Annotation queryParameterAnnotation : queryParameterAnnotations) {
                    if(queryParameterAnnotation.annotationType().isAssignableFrom(QueryParameter.class)) {
                        queryParameters[i] = ((QueryParameter)queryParameterAnnotation);
                    }
                }
            }
        }
    }

    @Override
    public Object execute(EntityManagerHolder entityManagerHolder, Object[] params) {
        entityManagerHolder.beginTransaction();
        Query query = entityManagerHolder.getEntityManager().createQuery(queryString);
        for(int i=0; i< params.length; i++) {
            if(queryParameters[i] != null) {
                query.setParameter(queryParameters[i].value(), params[i]);
            }
        }
        Collection<?> result = query.getResultList();
        if(!method.getReturnType().isAssignableFrom(Collection.class)) {
            if(result.size() == 1) {
                return result.iterator().next();
            } else if(result.size() == 0) {
                return null;
            } else {
                throw new IllegalStateException("Expected one result but got " + result.size());
            }
        }
        entityManagerHolder.commitTransaction();
        return result;
    }
    
}
