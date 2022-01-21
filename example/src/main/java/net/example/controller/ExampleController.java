package net.example.controller;

import java.util.Collection;

import javax.inject.Inject;

import net.example.ExampleBean;
import net.example.ExampleService;
import net.example.ExampleService2Impl;
import net.nnwsf.controller.annotation.Controller;
import net.nnwsf.controller.annotation.Delete;
import net.nnwsf.controller.annotation.Get;
import net.nnwsf.controller.annotation.PathVariable;
import net.nnwsf.controller.annotation.Post;
import net.nnwsf.controller.annotation.Put;
import net.nnwsf.controller.annotation.RequestBody;
import net.nnwsf.controller.annotation.RequestParameter;

@Controller("/test")
public class ExampleController {

    @Inject
    private ExampleService service;

    @Inject
    private ExampleService2Impl service2;

    @Get("/")
    public String get() {
        return service.echo("Hello example");
    }

    @Post("/")
    public ExampleBean Post(@RequestBody ExampleBean data) {
        data.setName(data.getName() + "-response");
        return data;
    }

    @Get("/")
    public String getQuery(@RequestParameter("echo") String echo, String ignore) {
        return service2.echo(echo) + " ---" +  service.echo(echo);
    }

    @Get("/log/{aString}")
    public String getLog(@PathVariable("aString") String aString, String ignore) {
        return service.log(aString);
    }

    @Put("/example/{aString}")
    public ExampleBean createExample(@PathVariable("aString") String aString) {
        return service.createExample(aString);
    }
    @Post("/example/{id}")
    public ExampleBean updateExampple(@PathVariable("id") int id, @RequestBody ExampleBean data) {
        return service.saveExample(id, data );
    }

    @Delete("/example/{id}")
    public void deleteExampple(@PathVariable("id") int id) {
        service.deleteExample(id );
    }

    @Get("/example/")
    public Collection<ExampleBean> getExamples() {
        return service.getExamples();
    }
}