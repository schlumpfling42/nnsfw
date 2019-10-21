package net.example;

import net.nnwsf.Server;
import net.nnwsf.configuration.AnnotationConfiguration;
import net.nnwsf.configuration.ServerConfiguration;

@ServerConfiguration()
@AnnotationConfiguration("net.example")
public class Application {
    public static void main(String[] args) {
         Server.start(Application.class);
    }
}