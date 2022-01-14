package net.example;

import javax.inject.Inject;

import net.example.persistence.ExampleEntity;
import net.example.persistence.ExampleRepository;
import net.nnwsf.service.annotation.Service;

@Service
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

}
