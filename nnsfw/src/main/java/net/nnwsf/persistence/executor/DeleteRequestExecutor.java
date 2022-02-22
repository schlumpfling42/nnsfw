package net.nnwsf.persistence.executor;

import java.lang.reflect.Method;

import org.hibernate.reactive.mutiny.Mutiny.Session;

import io.smallrye.mutiny.Uni;

public class DeleteRequestExecutor extends Executor {

    public DeleteRequestExecutor(Class<?> entityClass, Class<?> idClass, Method method) {
        super(entityClass, idClass, method);
    }

    @Override
    public Uni<?> execute(Session session, Object[] params) {
        if(entityClass.isInstance(params[0])) {
            long start = System.currentTimeMillis();
            return session.remove(params[0]).chain(aVoid -> session.flush()).attachContext().map(itemWithContext -> {
                itemWithContext.context().put("SQL", method.getName() + ":" + (System.currentTimeMillis() - start));
                return itemWithContext.get();
            });
        } else {
            throw new IllegalArgumentException("Entity type doesn't match");
        }
    }
    
}
