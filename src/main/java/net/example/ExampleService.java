package net.example;

import javax.transaction.Transactional;

import net.nnwsf.service.Service;

@Service()
public interface ExampleService {
    @Transactional
    String echo(String echo);
}
