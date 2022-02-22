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
import net.nnwsf.util.ReflectionHelper;

public class ServiceInterceptor {
    @RuntimeType
    public static Uni<?> intercept(@Origin Method method,
            @SuperCall Callable<Uni<?>> callable) throws Exception {
        if (ReflectionHelper.findAnnotation(method, Transactional.class) != null) {
            return Uni.createFrom().context(context -> {
                if(!context.contains("session")) {
                    return PersistenceManager.createSession(Default.DATASOURCE_NAME).map(aSession -> {
                        context.put("session", aSession);
                        return aSession;
                    }).chain(session -> 
                        session.withTransaction(trx -> {
                            try {
                                return callable.call();
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

}
