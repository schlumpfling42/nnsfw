package net.example;

import net.nnwsf.service.Service;

@Service
public class ExampleServiceImpl implements ExampleService{
    public String echo(String echo) {
        return echo;
    }
}
