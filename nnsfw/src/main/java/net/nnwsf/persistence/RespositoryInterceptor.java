package net.nnwsf.persistence;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

import javax.persistence.Id;
import javax.persistence.Query;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.nnwsf.persistence.annotation.QueryParameter;
import net.nnwsf.util.ReflectionHelper;

public class RespositoryInterceptor {
    private final Class<?> entityClass;
    private final Class<?> idClass;
    private final String datasourceName;

    public RespositoryInterceptor(Class<?> entityClass, String datasourceName) {
        this.entityClass = entityClass;
        Collection<Field> idFields = ReflectionHelper.findAnnotationFields(entityClass, Id.class);
        idClass = idFields.iterator().next().getType();
        this.datasourceName = datasourceName;
    }
    @RuntimeType
    public Object intercept(
        @Origin Method method,
        @AllArguments Object[] args
    ) throws Exception {
        net.nnwsf.persistence.annotation.Query queryAnnotation = method.getAnnotation(net.nnwsf.persistence.annotation.Query.class);
        if(queryAnnotation != null) {
            try(EntityManagerHolder entityManagerHolder = PersistenceManager.createEntityManager(datasourceName)) {
                entityManagerHolder.beginTransaction();
                Query query = entityManagerHolder.getEntityManager().createQuery(queryAnnotation.value());
                if(args.length > 0) {
                    Annotation[][] parametersAnnotations = method.getParameterAnnotations();
                    for(int i=0; i< args.length; i++) {
                        Annotation[] queryParameterAnnotations = parametersAnnotations[i];
                        if(queryParameterAnnotations != null) {
                            for(Annotation queryParameterAnnotation : queryParameterAnnotations) {
                                if(queryParameterAnnotation.annotationType().isAssignableFrom(QueryParameter.class)) {
                                    query.setParameter(((QueryParameter)queryParameterAnnotation).value(), args[i]);
                                }
                            }
                        }
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
        } else {
            if("save".equals(method.getName()) && method.getParameterTypes().length == 1) {
                if(entityClass.isInstance(args[0])) {
                    try(EntityManagerHolder entityManagerHolder = PersistenceManager.createEntityManager(datasourceName)) {
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
                    try(EntityManagerHolder entityManagerHolder = PersistenceManager.createEntityManager(datasourceName)) {
                        entityManagerHolder.beginTransaction();
                        Object mergedObject = entityManagerHolder.getEntityManager().merge(args[0]);
                        entityManagerHolder.getEntityManager().remove(mergedObject);
                        entityManagerHolder.commitTransaction();
                        return null;
                    }
                } else {
                    throw new IllegalAccessException("enity type doesn't match");
                }
            }
            if("findById".equals(method.getName()) && method.getParameterTypes().length == 1) {
                if(idClass.isInstance(args[0])) {
                    try(EntityManagerHolder entityManagerHolder = PersistenceManager.createEntityManager(datasourceName)) {
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
                try(EntityManagerHolder entityManagerHolder = PersistenceManager.createEntityManager(datasourceName)) {
                    entityManagerHolder.beginTransaction();
                    Object result = entityManagerHolder.getEntityManager().createQuery("select e from " + entityClass.getSimpleName() + " e").getResultList();
                    entityManagerHolder.commitTransaction();
                    return result;
                }
            }
        }
        return null;
    }
}
