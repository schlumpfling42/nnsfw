package net.example;

import net.nnwsf.controller.Controller;
import net.nnwsf.controller.Get;
import net.nnwsf.controller.PathVariable;
import net.nnwsf.controller.RequestParameter;

@Controller("/")
public class ExampleController {
    @Get("/")
    public String get() {
        return "Hello example";
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