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
            long start = System.currentTimeMillis();
            return session.merge(params[0]).chain(savedEntity -> session.flush().replaceWith(savedEntity)).attachContext().map(itemWithContext -> {
                itemWithContext.context().put("SQL", method.getName() + ":" + (System.currentTimeMillis() - start));
                return itemWithContext.get();
            });
        } else {
            throw new IllegalArgumentException("Entity type doesn't match");
        }
    }
    
}
