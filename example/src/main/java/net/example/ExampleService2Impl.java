package net.example;

import java.util.Collection;
import java.util.List;

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

    public Collection<ExampleBean> getExamples() {
        ExampleBean newBean = new ExampleBean();
        newBean.setName("Test");
        return List.of(newBean);
    }

    public void deleteExample(int id) {
    }
}
