package net.example;

import org.hibernate.jpa.HibernatePersistenceProvider;

import net.nnwsf.ApplicationServer;
import net.nnwsf.configuration.AnnotationConfiguration;
import net.nnwsf.configuration.AuthenticatedResourcePath;
import net.nnwsf.configuration.Server;
import net.nnwsf.persistence.PersistenceConfiguration;
import net.nnwsf.persistence.Property;

@Server()
@AnnotationConfiguration("net.example")
@AuthenticatedResourcePath("/secure")
@PersistenceConfiguration(
    providerClass=HibernatePersistenceProvider.class,
    jdbcDriver="org.h2.Driver",
    jdbcUrl="jdbc:h2:~/example",
    user="",
    password="",
    properties = {
        @Property(name = "hibernate.dialect", value = "org.hibernate.dialect.H2Dialect"),
        @Property(name = "hibernate.hbm2ddl.auto", value = "create-drop")
    }
)
public class Application {
    public static void main(String[] args) {
         ApplicationServer.start(Application.class);
    }
}