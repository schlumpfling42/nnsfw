package net.nnwsf.persistence;

import java.util.function.Consumer;

import javax.persistence.EntityManager;

public class EntityManagerHolder implements AutoCloseable {
    
    private final boolean created;
    private final EntityManager entityManager;
    private final Consumer<EntityManager> releaseFunction;

    public EntityManagerHolder(EntityManager entityManager, Consumer<EntityManager> releaseFunction, boolean created) {
        this.entityManager = entityManager;
        this.created = created;
        this.releaseFunction = releaseFunction;
    }

	public void beginTransaction() {
        if(!entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().begin();
        }
    }

    public void commitTransaction() {
        if(entityManager.getTransaction().isActive() && created) {
            entityManager.getTransaction().commit();
        }
    }

    public void rollbackTransaction() {
        if(entityManager.getTransaction().isActive() && created){
            entityManager.getTransaction().rollback();
        }
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public void close() throws Exception {
        if(created) {
            rollbackTransaction();
            entityManager.close();
            releaseFunction.accept(entityManager);
        }
    }
}