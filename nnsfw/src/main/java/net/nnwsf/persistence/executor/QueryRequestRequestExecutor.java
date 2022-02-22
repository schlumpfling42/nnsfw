package net.nnwsf.persistence.executor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

import org.hibernate.reactive.mutiny.Mutiny.Query;
import org.hibernate.reactive.mutiny.Mutiny.Session;

import io.smallrye.mutiny.Uni;
import net.nnwsf.persistence.annotation.QueryParameter;

public class QueryRequestRequestExecutor extends Executor {

    private final QueryParameter[] queryParameters;
    private final String queryString;

    public  QueryRequestRequestExecutor(Class<?> entityClass, Class<?> idClass, Method method) {
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
    public Uni<?> execute(Session session, Object[] params) {
        long start = System.currentTimeMillis();
        Query<Collection<?>> query = session.createQuery(queryString);
        for(int i=0; i< params.length; i++) {
            if(queryParameters[i] != null) {
                query.setParameter(queryParameters[i].value(), params[i]);
            }
        }
        return query.getResultList().chain(resultList -> {
            if(!method.getReturnType().isAssignableFrom(Collection.class)) {
                if(resultList.size() == 1) {
                    return Uni.createFrom().item(resultList.iterator().next());
                } else if(resultList.size() == 0) {
                    return Uni.createFrom().nullItem();
                }
            }
            return Uni.createFrom().failure(new IllegalStateException("Expected one result but got " + resultList.size()));
        }).attachContext().map(itemWithContext -> {
            itemWithContext.context().put("SQL", method.getName() + ":" + (System.currentTimeMillis() - start));
            return itemWithContext.get();
        });
    }
    
}
