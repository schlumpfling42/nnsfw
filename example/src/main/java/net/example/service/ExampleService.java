package net.example.service;

import java.util.Collection;

import javax.transaction.Transactional;

import net.example.resource.ExampleBean;
import net.nnwsf.resource.Page;
import net.nnwsf.resource.PageRequest;
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

    public Page<ExampleBean> getExamples(PageRequest pageRequest);
}
