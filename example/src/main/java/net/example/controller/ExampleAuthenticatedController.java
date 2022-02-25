package net.example.controller;

import net.example.resource.ExampleBean;
import net.nnwsf.authentication.User;
import net.nnwsf.authentication.annotation.Authenticated;
import net.nnwsf.controller.annotation.AuthenticatedUser;
import net.nnwsf.controller.annotation.Controller;
import net.nnwsf.controller.annotation.Get;
import net.nnwsf.controller.annotation.PathVariable;
import net.nnwsf.controller.annotation.Post;
import net.nnwsf.controller.annotation.RequestBody;

@Controller("/auth")
@Authenticated
public class ExampleAuthenticatedController {
    @Get("/")
    public String get(@AuthenticatedUser() User user) {
        return "Hello example. User = " + user.getDisplayName();
    }

    @Post("/")
    public ExampleBean Post(@RequestBody ExampleBean data) {
        data.setName(data.getName() + "-response");
        return data;
    }

    @Get("/:echo")
    public String getRequest(@PathVariable("echo") String echo, String ignore) {
        return echo;
    }
}