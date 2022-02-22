package net.example.service;

import java.util.Collection;

import javax.transaction.Transactional;

import io.smallrye.mutiny.Uni;
import net.example.resource.ExampleBean;
import net.nnwsf.resource.Page;
import net.nnwsf.resource.PageRequest;
import net.nnwsf.service.annotation.Service;

@Service
public interface ExampleService {
    
    @Transactional
    Uni<String> echo(String echo);

    Uni<String> log(String aString);

    // @Transactional
    // Uni<ExampleBean> createExample(String name);

    // @Transactional
    // Uni<ExampleBean> saveExample(int id, ExampleBean exampleBean);

    // @Transactional
    // Uni<Void> deleteExample(int id);

    // Uni<Page<ExampleBean>> getExamples(PageRequest pageRequest);
}
