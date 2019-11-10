package net.example;

import net.nnwsf.authentication.Authenticated;
import net.nnwsf.controller.*;

import java.security.Principal;

@Controller("/auth")
@Authenticated
public class ExampleAuthenticatedController {
    @Get("/")
    public String get(@AuthenticationPrincipal() Principal principal) {
        return "Hello example. User = " + principal.getName();
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