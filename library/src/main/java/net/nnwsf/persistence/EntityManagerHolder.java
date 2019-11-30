package net.nnwsf.persistence;

import javax.persistence.EntityManager;

public class EntityManagerHolder implements AutoCloseable {
    
    private final boolean created;
    private EntityManager entityManager;

    EntityManagerHolder(EntityManager entityManager, boolean created) {
        this.entityManager = entityManager;
        this.created = created;
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
        }
    }
}