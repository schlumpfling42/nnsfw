package net.example;

import java.util.Collection;

import javax.transaction.Transactional;

import net.nnwsf.service.annotation.Service;

@Service
public interface ExampleService {
    
    @Transactional
    String echo(String echo);

    String log(String aString);

    @Transactional
    ExampleBean createExample(String name);

    @Transactional
    ExampleBean saveExample(int id, ExampleBean exampleBean);

    @Transactional
    void deleteExample(int id);

    public Collection<ExampleBean> getExamples();
}
