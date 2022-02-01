package net.example.controller;

import java.util.Collection;

import javax.inject.Inject;

import net.example.ExampleBean;
import net.example.ExampleService;
import net.example.ExampleService2Impl;
import net.nnwsf.application.Constants;
import net.nnwsf.controller.annotation.ContentType;
import net.nnwsf.controller.annotation.Controller;
import net.nnwsf.controller.annotation.Delete;
import net.nnwsf.controller.annotation.Get;
import net.nnwsf.controller.annotation.PathVariable;
import net.nnwsf.controller.annotation.Post;
import net.nnwsf.controller.annotation.Put;
import net.nnwsf.controller.annotation.RequestBody;
import net.nnwsf.controller.annotation.RequestParameter;
import net.nnwsf.controller.documentation.annotation.ApiDoc;

@Controller("/test")
@ApiDoc("Example controller for showing the abilities of NNSFW")
public class ExampleController {

    @Inject
    private ExampleService service;

    @Inject
    private ExampleService2Impl service2;

    @Get("/")
    @ContentType(Constants.CONTENT_TYPE_TEXT)
    @ApiDoc("Simple get to show service injection")
    public String get() {
        return service.echo("Hello example");
    }

    @Post("/")
    @ContentType(Constants.CONTENT_TYPE_TEXT)
    @ApiDoc("Simple post to show how serialization works")
    public ExampleBean Post(@RequestBody ExampleBean data) {
        data.setName(data.getName() + "-response");
        return data;
    }

    @Get("/")
    @ContentType(Constants.CONTENT_TYPE_TEXT)
    @ApiDoc("Simple get to how query parameters are handled")
    public String getQuery(@RequestParameter("echo") String echo, String ignore) {
        return service2.echo(echo) + " ---" +  service.echo(echo);
    }

    @Get("/log/{aString}")
    @ContentType(Constants.CONTENT_TYPE_TEXT)
    @ApiDoc("Simple get to how path variables are handled")
    public String getLog(@PathVariable("aString") String aString, String ignore) {
        return service.log(aString);
    }

    @Put("/example/{aString}")
    @ContentType(Constants.CONTENT_TYPE_APPLICATION_JSON)
    @ApiDoc("Example put to create a new entry in the database, to show how the persistence integration works")
    public ExampleBean createExample(@PathVariable("aString") String aString) {
        return service.createExample(aString);
    }
    @Post("/example/{id}")
    @ContentType(Constants.CONTENT_TYPE_APPLICATION_JSON)
    @ApiDoc("Example post to update an entry in the database, to show how the persistence integration works")
    public ExampleBean updateExample(@PathVariable("id") int id, @RequestBody ExampleBean data) {
        return service.saveExample(id, data );
    }
    
    @Delete("/example/{id}")
    @ContentType(Constants.CONTENT_TYPE_APPLICATION_JSON)
    @ApiDoc("Example delete to delete an entry in the database, to show how the persistence integration works")
    public void deleteExample(@PathVariable("id") int id) {
        service.deleteExample(id );
    }
    
    @Get("/example/")
    @ContentType(Constants.CONTENT_TYPE_APPLICATION_JSON)
    @ApiDoc("Example get all entries from the database, to show how the persistence integration works")
    public Collection<ExampleBean> getExamples() {
        return service.getExamples();
    }
}