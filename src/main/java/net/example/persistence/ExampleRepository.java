package net.example.persistence;

import java.util.Collection;

import net.nnwsf.persistence.Repository;

@Repository(ExampleEntity.class)
public interface ExampleRepository {
    ExampleEntity save(ExampleEntity example);
    ExampleEntity findById(Integer id);
    Collection<ExampleEntity> findAll();
    void delete(ExampleEntity example);
}