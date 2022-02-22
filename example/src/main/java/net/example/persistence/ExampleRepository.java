package net.example.persistence;

import java.util.Collection;

import io.smallrye.mutiny.Uni;
import net.nnwsf.persistence.PersistenceRepository;
import net.nnwsf.persistence.annotation.Query;
import net.nnwsf.persistence.annotation.QueryParameter;
import net.nnwsf.persistence.annotation.Repository;

@Repository(entityClass = ExampleEntity.class)
public interface ExampleRepository extends PersistenceRepository<ExampleEntity, Integer>{
    @Query("select e from ExampleEntity e")
    Uni<Collection<ExampleEntity>> getAllWithQuery();

    @Query("select e from ExampleEntity e where e.id = :id")
    Uni<ExampleEntity> findById(@QueryParameter("id") Integer id);
}