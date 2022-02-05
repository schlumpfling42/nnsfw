package net.nnwsf.persistence;

import java.util.Collection;

import net.nnwsf.resource.Page;
import net.nnwsf.resource.PageRequest;

public interface PersistenceRepository<E, I> {
    E save(E entity);
    E findById(I id);
    Collection<E> findAll();
    Page<E> find(PageRequest request);
    void delete(E entity);
}