package net.example.service;

import java.util.Collection;
import java.util.List;

import net.example.resource.ExampleBean;
import net.nnwsf.resource.Page;
import net.nnwsf.resource.PageRequest;
import net.nnwsf.service.annotation.Service;

@Service("2")
public class ExampleService2Impl implements ExampleService {
    public String echo(String echo) {
        return echo;
    }

    public String log(String echo) {
        return echo;
    }

    public ExampleBean createExample(String name) {
        ExampleBean newBean = new ExampleBean();
        newBean.setName(name);
        return newBean;
    }

    public ExampleBean saveExample(int id, ExampleBean bean) {
        return bean;
    }

    public Page<ExampleBean> getExamples(PageRequest pageRequest) {
        ExampleBean newBean = new ExampleBean();
        newBean.setName("Test");
        return new Page<ExampleBean>(1, pageRequest, List.of(newBean));
    }

    public void deleteExample(int id) {
    }
}
