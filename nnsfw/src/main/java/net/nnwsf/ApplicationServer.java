package net.nnwsf;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import net.nnwsf.authentication.AuthenticationMechanism;
import net.nnwsf.configuration.AnnotationConfiguration;
import net.nnwsf.configuration.AuthenticatedResourcePath;
import net.nnwsf.configuration.ConfigurationManager;
import net.nnwsf.configuration.Server;
import net.nnwsf.controller.Controller;
import net.nnwsf.handler.HttpHandlerImpl;
import net.nnwsf.persistence.Datasource;
import net.nnwsf.persistence.PersistenceManager;
import net.nnwsf.service.ServiceManager;
import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.ReflectionHelper;

public class ApplicationServer {

    public static void main(String[] args) {
        try {
            if(args.length == 1) {
                start(Class.forName(args[0]));
            }
        } catch(Exception e) {

        }
        System.out.println("Usage: ApplicationServer <server class>");
        
        System.exit(-1);
    }

    private static final Logger log = Logger.getLogger(ApplicationServer.class.getName());

    private static ApplicationServer instance;

    public static ApplicationServer start(Class<?> applicationClass) {
        ConfigurationManager.init(applicationClass.getClassLoader());
        try {
            LogManager.getLogManager().readConfiguration(applicationClass.getClassLoader().getResourceAsStream("logging.properties"));
        } catch(Exception e) {
            System.err.println("Unable to read log configuration");
            e.printStackTrace();
        }
        log.log(Level.INFO, "Starting server application {0}", applicationClass);
        instance = new ApplicationServer(applicationClass);
        return instance;
    }

    private ApplicationServer(Class<?> applicationClass) {

        Server serverConfiguration = ReflectionHelper.findAnnotation(applicationClass, Server.class);
        serverConfiguration = ConfigurationManager.apply(serverConfiguration);

        String hostname = serverConfiguration.hostname();
        int port = serverConfiguration.port();
        String resourcePath = serverConfiguration.resourcePath();

        AnnotationConfiguration annotationConfiguration = ReflectionHelper.findAnnotation(applicationClass, AnnotationConfiguration.class);

        ClassDiscovery.init(applicationClass.getClassLoader(), annotationConfiguration.value());

        initServices();
        
        initPersistence(applicationClass);
            
            Collection<String> authenticatedResourcePaths = ReflectionHelper
            .findAnnotations(applicationClass, AuthenticatedResourcePath.class)
            .stream().map(annotation -> annotation.value())
            .collect(Collectors.toList());
            HttpHandler httpHandler;
            try {
                Collection<Class<Object>> controllerClasses = ClassDiscovery.discoverAnnotatedClasses(Object.class, Controller.class).values();
                Collection<Class<io.undertow.security.api.AuthenticationMechanism>> authenticationMechanimsClasses = ClassDiscovery.discoverAnnotatedClasses(io.undertow.security.api.AuthenticationMechanism.class, AuthenticationMechanism.class).values();
                httpHandler = new HttpHandlerImpl(applicationClass.getClassLoader(), resourcePath, authenticatedResourcePaths, controllerClasses, authenticationMechanimsClasses);
            } catch(Exception e) {
                throw new RuntimeException("Unable to discover annotated classes", e);
            }
            
            

        log.info("Starting server at " + hostname + " port " + port);
        Undertow server = Undertow.builder()
                .addHttpListener(port, hostname)
                .setHandler(httpHandler)
                .build();
        server.start();
    }
    
    @SuppressWarnings("unchecked")
    private void initPersistence(Class<?> applicationClass) {
        Datasource datasource = ReflectionHelper.findAnnotation(applicationClass, Datasource.class);
        datasource = ConfigurationManager.apply(datasource);
        PersistenceManager.init(
            datasource.providerClass(), 
            datasource.jdbcDriver(), 
            datasource.jdbcUrl(), 
            datasource.user(), 
            datasource.password(), 
            (Map<String, Object>)Optional.ofNullable(datasource.properties())
                .map(p -> 
                    Arrays.stream(p)
                    .collect(Collectors.toMap(v -> v.name(), v -> v.value()))
                ).orElse(Collections.EMPTY_MAP)
        );
    }

    private void initServices() {
        ServiceManager.init(ClassDiscovery.getPackagesToScan());
    }

}