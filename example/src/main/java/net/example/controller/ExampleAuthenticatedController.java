package net.example.controller;

import net.example.ExampleBean;
import net.nnwsf.authentication.Authenticated;
import net.nnwsf.authentication.User;
import net.nnwsf.controller.*;

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

    @Get("/")
    public String getQuery(@RequestParameter("echo") String echo, String ignore) {
        return echo;
    }

    @Get("/{echo}")
    public String getRequest(@PathVariable("echo") String echo, String ignore) {
        return echo;
    }
}