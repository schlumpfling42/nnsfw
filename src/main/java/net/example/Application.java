package net.example;

import net.nnwsf.ApplicationServer;
import net.nnwsf.configuration.AnnotationConfiguration;
import net.nnwsf.configuration.AuthenticatedResourcePath;
import net.nnwsf.configuration.Server;

@Server()
@AnnotationConfiguration("net.example")
@AuthenticatedResourcePath("/secure")
public class Application {
    public static void main(String[] args) {
         ApplicationServer.start(Application.class);
    }
}