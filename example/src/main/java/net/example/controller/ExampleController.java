package net.example.controller;

import javax.inject.Inject;

import io.smallrye.mutiny.Uni;
import net.example.resource.ExampleBean;
import net.example.service.ExampleService;
import net.example.service.ExampleService2Impl;
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
import net.nnwsf.resource.Page;
import net.nnwsf.resource.PageRequest;

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
    public Uni<String> get() {
        return service.echo("Hello example");
    }

    @Post("/")
    @ContentType(Constants.CONTENT_TYPE_TEXT)
    @ApiDoc("Simple post to show how serialization works")
    public Uni<ExampleBean> Post(@RequestBody ExampleBean data) {
        data.setName(data.getName() + "-response");
        return Uni.createFrom().item(data);
    }

    @Get("/")
    @ContentType(Constants.CONTENT_TYPE_TEXT)
    @ApiDoc("Simple get to how query parameters are handled")
    public String getQuery(@RequestParameter("echo") String echo, String ignore) {
        return service2.echo(echo) + " ---" +  service.echo(echo);
    }

    @Get("/log/:aString")
    @ContentType(Constants.CONTENT_TYPE_TEXT)
    @ApiDoc("Simple get to how path variables are handled")
    public Uni<String> getLog(@PathVariable("aString") String aString, String ignore) {
        return service.log(aString);
    }

    @Put("/example/:aString")
    @ContentType(Constants.CONTENT_TYPE_APPLICATION_JSON)
    @ApiDoc("Example put to create a new entry in the database, to show how the persistence integration works")
    public Uni<ExampleBean> createExample(@PathVariable("aString") String aString) {
        return service.createExample(aString);
    }
    @Post("/example/:id")
    @ContentType(Constants.CONTENT_TYPE_APPLICATION_JSON)
    @ApiDoc("Example post to update an entry in the database, to show how the persistence integration works")
    public Uni<ExampleBean> updateExample(@PathVariable("id") int id, @RequestBody ExampleBean data) {
        return service.saveExample(id, data );
    }
    
    @Delete("/example/:id")
    @ContentType(Constants.CONTENT_TYPE_APPLICATION_JSON)
    @ApiDoc("Example delete to delete an entry in the database, to show how the persistence integration works")
    public Uni<Void> deleteExample(@PathVariable("id") int id) {
        return service.deleteExample(id );
    }
    
    @Get("/example/")
    @ContentType(Constants.CONTENT_TYPE_APPLICATION_JSON)
    @ApiDoc("Example get all entries from the database, to show how the persistence integration works")
    public Uni<Page<ExampleBean>> getExamples(@RequestParameter("page") int page, @RequestParameter("size") int size) {
        return service.getExamples(PageRequest.of(page, size));
    }
}