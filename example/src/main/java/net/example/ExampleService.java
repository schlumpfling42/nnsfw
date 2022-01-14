package net.example;

import javax.transaction.Transactional;

import net.nnwsf.service.annotation.Service;

@Service
public interface ExampleService {
    
    @Transactional
    String echo(String echo);

    String log(String aString);
}
