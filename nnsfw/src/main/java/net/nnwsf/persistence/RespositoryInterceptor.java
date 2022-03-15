package net.nnwsf.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Id;

import org.hibernate.reactive.mutiny.Mutiny.Session;

import io.smallrye.mutiny.Uni;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.nnwsf.persistence.executor.DeleteRequestExecutor;
import net.nnwsf.persistence.executor.Executor;
import net.nnwsf.persistence.executor.FindByIdRequestExecutor;
import net.nnwsf.persistence.executor.FindWithPageRequestExecutor;
import net.nnwsf.persistence.executor.QueryRequestRequestExecutor;
import net.nnwsf.persistence.executor.SaveRequestExecutor;
import net.nnwsf.query.SearchTerm;
import net.nnwsf.resource.PageRequest;
import net.nnwsf.util.ReflectionHelper;

public class RespositoryInterceptor {
    private final String datasourceName;
    private final Map<Method, Executor> executors;

    public RespositoryInterceptor(Class<?> entityClass, Class<?> repositoryClass, String datasourceName) {
        Collection<Field> idFields = ReflectionHelper.findAnnotationFields(entityClass, Id.class);
        Field idField = idFields.iterator().next();
        Column column = ReflectionHelper.findAnnotation(idField, Column.class);
    
        final String idColumnName = column == null ? idField.getName() : column.name();

        Class<?> idClass = idFields.iterator().next().getType();
        this.datasourceName = datasourceName;
        executors = new HashMap<>();
        Arrays.stream(repositoryClass.getMethods()).forEach(aMethod -> {
            if("save".equals(aMethod.getName()) && aMethod.getParameterCount() == 1) {
                executors.put(aMethod, new SaveRequestExecutor(entityClass, idClass, aMethod));
            } else if("findById".equals(aMethod.getName()) && aMethod.getParameterCount() == 1) {
                executors.put(aMethod, new FindByIdRequestExecutor(entityClass, idClass, aMethod));
            } else if("find".equals(aMethod.getName()) && aMethod.getParameterCount() == 2 && PageRequest.class.equals(aMethod.getParameterTypes()[0]) && SearchTerm.class.equals(aMethod.getParameterTypes()[1])) {
                executors.put(aMethod, new FindWithPageRequestExecutor(entityClass, idClass, idColumnName, aMethod));
            } else if("delete".equals(aMethod.getName()) && aMethod.getParameterCount() == 1) {
                executors.put(aMethod, new DeleteRequestExecutor(entityClass, idClass, aMethod));
            } else {
                net.nnwsf.persistence.annotation.Query queryAnnotation = aMethod.getAnnotation(net.nnwsf.persistence.annotation.Query.class);
                if(queryAnnotation != null) {
                    executors.put(aMethod, new QueryRequestRequestExecutor(entityClass, idClass, aMethod));
                }
            }
        });
    }
    @RuntimeType
    public Uni<?> intercept(@Origin Method method, @AllArguments Object[] args) throws Exception {
        Executor executor = executors.get(method);
        if(executor != null) {
            return Uni.createFrom().context(context -> {
                    if(!context.contains("session")) {
                        return PersistenceManager.createSession(datasourceName).map(aSession -> {
                            context.put("session", aSession);
                            return aSession;
                        }).chain(session -> session.withTransaction(trx -> 
                                executor.execute(session, args)
                            )
                            .chain(result -> {
                                context.delete("session");
                                return session.close().replaceWith(result);
                            }));
                    }
                    return Uni.createFrom().item(() -> (Session)context.get("session")).chain(session -> executor.execute(session, args));
                });
        } else {
            throw new UnsupportedOperationException("Unable to execute: " + method.getName());
        }
    }
}
