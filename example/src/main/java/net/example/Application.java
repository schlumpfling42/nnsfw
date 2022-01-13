package net.example;

import net.nnwsf.ApplicationServer;
import net.nnwsf.authentication.Authenticated;
import net.nnwsf.configuration.AnnotationConfiguration;
import net.nnwsf.configuration.AuthenticatedResourcePathConfiguration;
import net.nnwsf.configuration.AuthenticationProviderConfiguration;
import net.nnwsf.configuration.ServerConfiguration;
import net.nnwsf.persistence.DatasourceConfiguration;

@ServerConfiguration
@AnnotationConfiguration("net.example")
@AuthenticatedResourcePathConfiguration("/authenticated")
@AuthenticationProviderConfiguration
@DatasourceConfiguration
public class Application {
    public static void main(String[] args) {
         ApplicationServer.start(Application.class);
    }
}