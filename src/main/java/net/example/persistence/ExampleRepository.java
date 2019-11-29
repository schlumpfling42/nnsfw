package net.example.persistence;

import net.nnwsf.persistence.Repository;

@Repository(ExampleEntity.class)
public interface ExampleRepository {
    ExampleEntity save(ExampleEntity example);
    ExampleEntity findById(Integer id);
}