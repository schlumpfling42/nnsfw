package net.example.service;

import java.util.Collection;
import java.util.List;

import javax.transaction.Transactional;

import io.smallrye.mutiny.Uni;
import net.example.resource.ExampleBean;
import net.nnwsf.resource.Page;
import net.nnwsf.resource.PageRequest;
import net.nnwsf.service.annotation.Service;

@Service("2")
public class ExampleService2Impl implements ExampleService {
    public Uni<String> echo(String echo) {
        return Uni.createFrom().item(echo);
    }

    public Uni<String> log(String echo) {
        return Uni.createFrom().item(echo);
    }

    public Uni<ExampleBean> createExample(String name) {
        ExampleBean newBean = new ExampleBean();
        newBean.setName(name);
        return Uni.createFrom().item(newBean);
    }

    public Uni<ExampleBean> saveExample(int id, ExampleBean bean) {
        return Uni.createFrom().item(bean);
    }

    public Uni<Page<ExampleBean>> getExamples(PageRequest pageRequest) {
        ExampleBean newBean = new ExampleBean();
        newBean.setName("Test");
        return Uni.createFrom().item(new Page<ExampleBean>(1, pageRequest, List.of(newBean)));
    }

    public Uni<Void> deleteExample(int id) {
        return Uni.createFrom().voidItem();
    }
}
