package net.example.persistence;

import java.util.Collection;

import net.nnwsf.persistence.Repository;
import net.nnwsf.persistence.PersistenceRepository;

@Repository(ExampleEntity.class)
public interface ExampleRepository extends PersistenceRepository<ExampleEntity, Integer>{
}