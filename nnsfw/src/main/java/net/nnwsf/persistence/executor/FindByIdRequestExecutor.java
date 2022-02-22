package net.nnwsf.persistence.executor;

import java.lang.reflect.Method;

import org.hibernate.reactive.mutiny.Mutiny.Session;

import io.smallrye.mutiny.Uni;


public class FindByIdRequestExecutor extends Executor {

    public FindByIdRequestExecutor(Class<?> entityClass, Class<?> idClass, Method method) {
        super(entityClass, idClass, method);
    }

    @Override
    public Uni<?> execute(Session session, Object[] params) {
        if(idClass.isInstance(params[0])) {
            long start = System.currentTimeMillis();
            return session.find(entityClass, params[0]).attachContext().map(itemWithContext -> {
                itemWithContext.context().put("SQL", method.getName() + ":" + (System.currentTimeMillis() - start));
                return itemWithContext.get();
            });
        } else {
            throw new IllegalArgumentException("id type doesn't match");
        }
    }
    
}
