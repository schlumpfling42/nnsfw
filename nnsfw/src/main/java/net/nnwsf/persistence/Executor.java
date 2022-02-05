package net.nnwsf.persistence;

import java.lang.reflect.Method;

public abstract class Executor {
    protected final Class<?> entityClass;
    protected final Class<?> idClass;
    protected final Method method;

    Executor(Class<?> entityClass, Class<?> idClass, Method method) {
        this.entityClass = entityClass;
        this.idClass = idClass;
        this.method = method;

    }
    public abstract Object execute(EntityManagerHolder entityManagerHolder, Object[] params);
}
