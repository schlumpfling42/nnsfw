package net.example.controller;

import javax.inject.Inject;

import net.example.ExampleBean;
import net.example.ExampleService;
import net.example.ExampleService2Impl;
import net.nnwsf.controller.Controller;
import net.nnwsf.controller.Get;
import net.nnwsf.controller.PathVariable;
import net.nnwsf.controller.Post;
import net.nnwsf.controller.RequestBody;
import net.nnwsf.controller.RequestParameter;

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

    @Get("/{echo}")
    public String getRequest(@PathVariable("echo") String echo, String ignore) {
        return service.echo(echo);
    }
}