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

    // public Uni<ExampleBean> createExample(String name) {
    //     ExampleEntity entity = new ExampleEntity();
    //     entity.setName(name);
    //     entity = repository.save(entity);
    //     entity = repository.findById(entity.getId());
    //     ExampleBean exampleBean = new ExampleBean();
    //     exampleBean.setName(entity.getName());
    //     exampleBean.setId(entity.getId());
    //     return exampleBean;
    // }

    // public Uni<ExampleBean> saveExample(int id, ExampleBean bean) {
    //     ExampleEntity entity = repository.findById(id);
    //     entity.setName(bean.getName());
    //     entity = repository.save(entity);
    //     ExampleBean exampleBean = new ExampleBean();
    //     exampleBean.setName(entity.getName());
    //     exampleBean.setId(entity.getId());
    //     return exampleBean;
    // }

    // public Uni<Void> deleteExample(int id) {
    //     ExampleEntity entity = repository.findById(id);
    //     repository.delete(entity);
    // }
    
    // public Uni<Page<ExampleBean>> getExamples(PageRequest pageRequest) {
    //     Page<ExampleEntity> resultPage = repository.find(pageRequest, null);
    //     Collection<ExampleBean> exampleBeans = resultPage.getElements().stream().map(entity -> {
    //         ExampleBean exampleBean = new ExampleBean();
    //         exampleBean.setName(entity.getName());
    //         exampleBean.setId(entity.getId());
    //         return exampleBean;
    //     }).collect(Collectors.toList());
    //     return new Page<ExampleBean>(resultPage.getTotalNumber(), pageRequest, exampleBeans);
    // }

}
