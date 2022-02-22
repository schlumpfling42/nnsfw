package net.nnwsf.persistence;

import java.util.Collection;

import io.smallrye.mutiny.Uni;
import net.nnwsf.query.SearchTerm;
import net.nnwsf.resource.Page;
import net.nnwsf.resource.PageRequest;

public interface PersistenceRepository<E, I> {
    Uni<E> save(E entity);
    Uni<E> findById(I id);
    Uni<Collection<E>> findAll();
    Uni<Page<E>> find(PageRequest request, SearchTerm searchTerm);
    Uni<Void> delete(E entity);
}