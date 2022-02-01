package net.example;

import net.nnwsf.application.ApplicationServer;
import net.nnwsf.application.annotation.AnnotationConfiguration;
import net.nnwsf.application.annotation.ApiDocConfiguration;
import net.nnwsf.application.annotation.AuthenticatedResourcePathConfiguration;
import net.nnwsf.application.annotation.AuthenticationProviderConfiguration;
import net.nnwsf.application.annotation.DatasourceConfiguration;
import net.nnwsf.application.annotation.FlywayConfiguration;
import net.nnwsf.application.annotation.ServerConfiguration;

@ServerConfiguration
@AnnotationConfiguration("net.example")
@AuthenticatedResourcePathConfiguration("/authenticated")
@AuthenticationProviderConfiguration
@DatasourceConfiguration
@FlywayConfiguration
@ApiDocConfiguration
public class Application {
    public static void main(String[] args) {
         ApplicationServer.start(Application.class);
    }
}