package net.example.persistence;

import java.util.Collection;

import net.nnwsf.persistence.Repository;
import net.nnwsf.persistence.PersistenceRepository;
import net.nnwsf.persistence.Query;
import net.nnwsf.persistence.QueryParameter;

@Repository(entityClass = ExampleEntity.class)
public interface ExampleRepository extends PersistenceRepository<ExampleEntity, Integer>{
    @Query("select e from ExampleEntity e")
    Collection<ExampleEntity> getAllWithQuery();

    @Query("select e from ExampleEntity e where e.id = :id")
    ExampleEntity findById(@QueryParameter("id") Integer id);
}