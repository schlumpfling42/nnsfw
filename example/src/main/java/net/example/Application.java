package net.example;

import net.nnwsf.ApplicationServer;
import net.nnwsf.configuration.AnnotationConfiguration;
import net.nnwsf.configuration.AuthenticatedResourcePath;
import net.nnwsf.configuration.Server;
import net.nnwsf.persistence.Datasource;

@Server()
@AnnotationConfiguration("net.example")
@AuthenticatedResourcePath("/secure")
@Datasource()
public class Application {
    public static void main(String[] args) {
         ApplicationServer.start(Application.class);
    }
}