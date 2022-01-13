package net.example;

import net.nnwsf.service.Service;

@Service("2")
public class ExampleService2Impl implements ExampleService {
    public String echo(String echo) {
        return echo;
    }
}
