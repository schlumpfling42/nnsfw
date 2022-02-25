package net.nnwsf.service;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.transaction.Transactional;

import org.hibernate.reactive.mutiny.Mutiny.Session;

import io.smallrye.mutiny.Uni;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.nnwsf.configuration.Default;
import net.nnwsf.persistence.PersistenceManager;
import net.nnwsf.service.annotation.Datasource;
import net.nnwsf.util.ReflectionHelper;

public class ServiceInterceptor {

    private final Collection<Method> methods;
    private final Map<Method, Transactional> methodTransactionals;
    private final Datasource datasource;
    private final Map<Method, Datasource> methodDatasources;

    ServiceInterceptor(Class<?> serviceClass) {
        methods = new HashSet<>();
        methodTransactionals = new HashMap<>();
        methodDatasources = new HashMap<>();
        datasource = ReflectionHelper.findAnnotation(serviceClass, Datasource.class);
    }


    @RuntimeType
    public Uni<?> intercept(@Origin Method method,
            @SuperCall Callable<Uni<?>> callable) throws Exception {
        check(method);
        Transactional transactional = getTransactional(method);
        Datasource datasource = getDatasource(method);
        if (transactional != null) {
            return Uni.createFrom().context(context -> {
                if(!context.contains("session")) {
                    return PersistenceManager.createSession(datasource == null ? Default.DATASOURCE_NAME : datasource.value()).map(aSession -> {
                        context.put("session", aSession);
                        return aSession;
                    }).chain(session -> 
                        session.withTransaction(trx -> {
                            try {
                                Object result = callable.call();
                                if(result == null) {
                                    return Uni.createFrom().nullItem();
                                } else if(result instanceof Uni) {
                                    return (Uni<Object>)result;
                                } else {
                                    return Uni.createFrom().item(result);
                                }
                            } catch (Exception e) {
                                trx.markForRollback();
                                return Uni.createFrom().failure(e);
                            }
                        }).chain(result -> {
                            context.delete("session");
                            return session.close().replaceWith(result);
                        })
                    );
                }
                return Uni.createFrom().item(() -> (Session)context.get("session")).chain(session -> session.withTransaction(trx -> {
                    try {
                        return callable.call();
                    } catch (Exception e) {
                        trx.markForRollback();
                        return Uni.createFrom().failure(e);
                    }
                }));

            });
        }
        return callable.call();
    }

    private void check(Method aMethod) {
        if(!methods.contains(aMethod)) {
            methods.add(aMethod);
            Transactional transactional = ReflectionHelper.findAnnotation(aMethod, Transactional.class);
            if(transactional != null) {
                methodTransactionals.put(aMethod, transactional);
            }
            Datasource datasource = ReflectionHelper.findAnnotation(aMethod, Datasource.class);
            if(datasource != null) {
                methodDatasources.put(aMethod, datasource);
            }
        }
    }

    private Transactional getTransactional(Method aMethod) {
        return methodTransactionals.get(aMethod);
    }

    private Datasource getDatasource(Method aMethod) {
        return this.datasource == null ? methodDatasources.get(aMethod) : datasource;
    }

}
