package net.example;

import javax.inject.Inject;
import javax.transaction.Transactional;

import net.example.persistence.ExampleEntity;
import net.example.persistence.ExampleRepository;
import net.nnwsf.service.Service;

@Service
public class ExampleServiceImpl implements ExampleService{

    @Inject
    private ExampleRepository repository;
    
    //@Transactional
    public String echo(String echo) {
        ExampleEntity entity = new ExampleEntity();
        entity.setName(echo);
        entity = repository.save(entity);
        entity = repository.findById(entity.getId());
        repository.delete(entity);
        return entity.getName() + ":" + repository.findAll().size();
    }
}
