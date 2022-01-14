package net.example;

import net.nnwsf.service.annotation.Service;

@Service("2")
public class ExampleService2Impl implements ExampleService {
    public String echo(String echo) {
        return echo;
    }

    public String log(String echo) {
        return echo;
    }
}
