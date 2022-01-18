package net.nnwsf.persistence;

import java.util.Collection;

import net.nnwsf.persistence.annotation.Query;
import net.nnwsf.persistence.annotation.QueryParameter;
import net.nnwsf.persistence.annotation.Repository;

@Repository(entityClass = TestEntity.class)
public interface TestRepository extends PersistenceRepository<TestEntity, Integer>{
    @Query("select e from TestEntity e")
    Collection<TestEntity> getAllWithQuery();

    @Query("select e from TestEntity e where e.id = :id")
    TestEntity findById(@QueryParameter("id") Integer id);
}
