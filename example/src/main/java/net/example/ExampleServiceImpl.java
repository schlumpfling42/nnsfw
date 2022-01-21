package net.example;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.inject.Inject;

import net.example.persistence.ExampleEntity;
import net.example.persistence.ExampleRepository;

public class ExampleServiceImpl implements ExampleService{

    @Inject
    private ExampleRepository repository;
    
    public String echo(String echo) {
        ExampleEntity entity = new ExampleEntity();
        entity.setName(echo);
        entity = repository.save(entity);
        entity = repository.findById(entity.getId());
        repository.delete(entity);
        return entity.getName() + ":" + repository.findAll().size();
    }

    public String log(String aString) {
        StringBuilder log = new StringBuilder();
        log.append("Before: ");
        log.append("Count: " + repository.findAll().size() + "\n");
        ExampleEntity entity = new ExampleEntity();
        entity.setName(aString);
        entity = repository.save(entity);
        log.append("Save: " + entity.getId() + ":" + entity.getName());
        log.append("Count: " + repository.findAll().size() + "\n");
        entity = repository.findById(entity.getId());
        repository.delete(entity);
        log.append("After: ");
        log.append("Count: " + repository.findAll().size() + "\n");
        return log.toString();
    }

    public ExampleBean createExample(String name) {
        ExampleEntity entity = new ExampleEntity();
        entity.setName(name);
        entity = repository.save(entity);
        entity = repository.findById(entity.getId());
        ExampleBean exampleBean = new ExampleBean();
        exampleBean.setName(entity.getName());
        exampleBean.setId(entity.getId());
        return exampleBean;
    }

    public ExampleBean saveExample(int id, ExampleBean bean) {
        ExampleEntity entity = repository.findById(id);
        entity.setName(bean.getName());
        entity = repository.save(entity);
        ExampleBean exampleBean = new ExampleBean();
        exampleBean.setName(entity.getName());
        exampleBean.setId(entity.getId());
        return exampleBean;
    }

    public void deleteExample(int id) {
        ExampleEntity entity = repository.findById(id);
        repository.delete(entity);
    }
    
    public Collection<ExampleBean> getExamples() {
        return repository.findAll().stream().map(entity -> {
            ExampleBean exampleBean = new ExampleBean();
            exampleBean.setName(entity.getName());
            exampleBean.setId(entity.getId());
            return exampleBean;
        }).collect(Collectors.toList());
    }

}
