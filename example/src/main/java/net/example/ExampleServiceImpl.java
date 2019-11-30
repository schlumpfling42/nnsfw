package net.example;

import javax.inject.Inject;

import net.example.persistence.ExampleEntity;
import net.example.persistence.ExampleRepository;
import net.nnwsf.service.Service;

@Service
public class ExampleServiceImpl implements ExampleService{

    @Inject
    private ExampleRepository repository;
    
    public String echo(String echo) {
        ExampleEntity entity = new ExampleEntity();
        entity.setName(echo);
        entity = repository.save(entity);
        entity = repository.findAll().iterator().next();
        repository.delete(entity);
        return entity.getName() + ":" + repository.findAll().size();
    }
}