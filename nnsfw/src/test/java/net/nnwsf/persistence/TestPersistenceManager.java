package net.nnwsf.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.nnwsf.application.TestApplication;
import net.nnwsf.service.annotation.Service;
import net.nnwsf.util.InjectionHelper;

public class TestPersistenceManager {

    @Service
    public static class InjectionService {
        @Inject
        TestRepository testRepository;
    }

    private InjectionService injectionService;

    @BeforeAll
    public static void setupBeforeAll() {
        TestApplication.init();
    }

    @BeforeEach
    public void setupEach() throws Exception {
        injectionService = InjectionHelper.getInjectable(InjectionService.class, null);
    }
    
    @Test
    public void testInsert() {
        TestEntity newTestEnity = new TestEntity();
        newTestEnity.setId(1);
        newTestEnity.setName(UUID.randomUUID().toString());
        TestEntity2 newTestEnity2 = new TestEntity2();
        newTestEnity2.setId(1);
        newTestEnity2.setName(UUID.randomUUID().toString());
        newTestEnity.setTest2List(List.of(newTestEnity2));
        try {
            newTestEnity = injectionService.testRepository.save(newTestEnity).await().indefinitely();
            Optional<TestEntity> findFirst = injectionService.testRepository.find(null, null).await().indefinitely().getElements().stream().findFirst();
            assertEquals(true, findFirst.isPresent());
            assertEquals(newTestEnity.getName(), findFirst.get().getName());
            assertEquals(1, findFirst.get().getTest2List().size());
            assertEquals(newTestEnity2.getName(), findFirst.get().getTest2List().get(0).getName());
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            injectionService.testRepository.delete(newTestEnity);
        }
    }
}