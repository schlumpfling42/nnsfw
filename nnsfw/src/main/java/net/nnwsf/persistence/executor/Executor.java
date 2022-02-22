package net.nnwsf.persistence.executor;

import java.lang.reflect.Method;

import org.hibernate.reactive.mutiny.Mutiny.Session;

import io.smallrye.mutiny.Uni;

public abstract class Executor {
    protected final Class<?> entityClass;
    protected final Class<?> idClass;
    protected final Method method;

    Executor(Class<?> entityClass, Class<?> idClass, Method method) {
        this.entityClass = entityClass;
        this.idClass = idClass;
        this.method = method;

    }
    public abstract Uni<?> execute(Session session, Object[] params);
}
