package net.nnwsf.persistence.executor;

import java.lang.reflect.Method;

import org.hibernate.reactive.mutiny.Mutiny.Session;

import io.smallrye.mutiny.Uni;

public class SaveRequestExecutor extends Executor {

    public SaveRequestExecutor(Class<?> entityClass, Class<?> idClass, Method method) {
        super(entityClass, idClass, method);
    }

    @Override
    public Uni<?> execute(Session session, Object[] params) {
        if(entityClass.isInstance(params[0])) {
            return session.merge(params[0]);
        } else {
            throw new IllegalArgumentException("Entity type doesn't match");
        }
    }
    
}
