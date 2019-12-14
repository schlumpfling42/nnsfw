package net.example;

import net.nnwsf.ApplicationServer;
import net.nnwsf.configuration.AnnotationConfiguration;
import net.nnwsf.configuration.AuthenticatedResourcePathConfiguration;
import net.nnwsf.configuration.ServerConfiguration;
import net.nnwsf.persistence.DatasourceConfiguration;

@ServerConfiguration()
@AnnotationConfiguration("net.example")
@AuthenticatedResourcePathConfiguration("/secure")
@DatasourceConfiguration()
public class Application {
    public static void main(String[] args) {
         ApplicationServer.start(Application.class);
    }
}