package net.example;

import org.hibernate.jpa.HibernatePersistenceProvider;

import net.nnwsf.ApplicationServer;
import net.nnwsf.configuration.AnnotationConfiguration;
import net.nnwsf.configuration.AuthenticatedResourcePath;
import net.nnwsf.configuration.Server;
import net.nnwsf.persistence.PersistenceConfiguration;

@Server()
@AnnotationConfiguration("net.example")
@AuthenticatedResourcePath("/secure")
@PersistenceConfiguration(
    providerClass=HibernatePersistenceProvider.class,
    jdbcDriver="org.h2.Driver",
    jdbcUrl="jdbc:h2:~/example",
    dialect = "org.hibernate.dialect.H2Dialect",
    user="",
    password=""
)
public class Application {
    public static void main(String[] args) {
         ApplicationServer.start(Application.class);
    }
}