package net.rka.server.fw.controller;

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