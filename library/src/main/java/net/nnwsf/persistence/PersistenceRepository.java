package net.nnwsf.persistence;

import java.util.Collection;

public interface PersistenceRepository<E, I> {
    E save(E example);
    E findById(I id);
    Collection<E> findAll();
    void delete(E example);
}