package net.nnwsf.service;

import java.lang.reflect.Method;
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

    private final Datasource datasource;

    ServiceInterceptor(Class<?> serviceClass) {
        datasource = ReflectionHelper.findAnnotation(serviceClass, Datasource.class);
    }

    @RuntimeType
    @SuppressWarnings("unchecked")
    public Uni<?> intercept(@Origin Method method,
            @SuperCall Callable<Uni<?>> callable) throws Exception {
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


    private Transactional getTransactional(Method aMethod) {
        return ReflectionHelper.findAnnotation(aMethod, Transactional.class);
    }

    private Datasource getDatasource(Method aMethod) {
        if(datasource == null) {
            return ReflectionHelper.findAnnotation(aMethod, Datasource.class);
        }
        return datasource;
    }

}
