package net.example;

import org.hibernate.jpa.HibernatePersistenceProvider;

import net.nnwsf.ApplicationServer;
import net.nnwsf.configuration.AnnotationConfiguration;
import net.nnwsf.configuration.AuthenticatedResourcePath;
import net.nnwsf.configuration.Server;
import net.nnwsf.persistence.PersistenceConfiguration;;

@Server()
@AnnotationConfiguration("net.example")
@AuthenticatedResourcePath("/secure")
@PersistenceConfiguration(
    providerClass=HibernatePersistenceProvider.class,
    jdbcDriver="org.postgresql.Driver",
    jdbcUrl="jdbc:postgresql://localhost/administration",
    user="postgres",
    password=""
)
public class Application {
    public static void main(String[] args) {
         ApplicationServer.start(Application.class);
    }
}