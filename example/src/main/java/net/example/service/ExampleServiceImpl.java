package net.example.service;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import io.smallrye.mutiny.Uni;
import net.example.persistence.ExampleEntity;
import net.example.persistence.ExampleRepository;
import net.example.resource.ExampleBean;
import net.nnwsf.resource.Page;
import net.nnwsf.resource.PageRequest;

public class ExampleServiceImpl implements ExampleService{

    @Inject
    private ExampleRepository repository;
    
    @Transactional
    public Uni<String> echo(String echo) {
        ExampleEntity entity = new ExampleEntity();
        entity.setName(echo);
        return repository.save(entity).chain(savedEntity -> repository.findById(savedEntity.getId()).chain(foundEntity ->
            repository.delete(foundEntity))).flatMap(ignore -> repository.findAll().map(resultList -> entity.getName() + ":" + resultList.size()));
    }

    @Transactional
    public Uni<String> log(String aString) {
        StringBuilder log = new StringBuilder();
        log.append("Before: ");
        return repository.findAll()
            .chain(resultList -> {
                log.append("Count: " + resultList.size() + "\n");
                ExampleEntity entity = new ExampleEntity();
                entity.setName(aString);
                return repository.save(entity);
            }).chain(savedEnity -> {
                log.append("Save: " + savedEnity.getId() + ":" + savedEnity.getName());
                return repository.findAll()
                    .chain(resultList -> {
                        log.append("Count: " + resultList.size() + "\n");
                        return repository.findById(savedEnity.getId());
                    });
            })
            .chain(foundEntity -> repository.delete(foundEntity))
            .chain(ignore -> {
                return repository.findAll().map(resultList -> {
                    log.append("After: ");
                    log.append("Count: " + resultList.size() + "\n");
                    return log.toString();
                });
            });
    }

    public Uni<ExampleBean> createExample(String name) {
        ExampleEntity entity = new ExampleEntity();
        entity.setName(name);
        return repository.save(entity).chain(savedEntity -> repository.findById(entity.getId()).map(foundEntity -> {
            ExampleBean exampleBean = new ExampleBean();
            exampleBean.setName(entity.getName());
            exampleBean.setId(entity.getId());
            return exampleBean;
        }));
    }

    public Uni<ExampleBean> saveExample(int id, ExampleBean bean) {
        return repository.findById(id).chain(foundEntity -> {
            foundEntity.setName(bean.getName());
            return repository.save(foundEntity).map(savedEntity -> {
                ExampleBean exampleBean = new ExampleBean();
                exampleBean.setName(savedEntity.getName());
                exampleBean.setId(savedEntity.getId());
                return exampleBean;
            });
        });
    }

    public Uni<Void> deleteExample(int id) {
        return repository.findById(id).chain(foundEntity ->
            repository.delete(foundEntity));
    }
    
    public Uni<Page<ExampleBean>> getExamples(PageRequest pageRequest) {
        return repository.find(pageRequest, null).map(resultPage -> {
            Collection<ExampleBean> exampleBeans = resultPage.getElements().stream().map(entity -> {
                ExampleBean exampleBean = new ExampleBean();
                exampleBean.setName(entity.getName());
                exampleBean.setId(entity.getId());
                return exampleBean;
            }).collect(Collectors.toList());
            return new Page<ExampleBean>(resultPage.getTotalNumber(), pageRequest, exampleBeans);
        });
    }

}
