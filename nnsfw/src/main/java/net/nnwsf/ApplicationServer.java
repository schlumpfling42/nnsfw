package net.nnwsf;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.persistence.spi.PersistenceProvider;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import net.nnwsf.authentication.AuthenticationMechanism;
import net.nnwsf.configuration.AnnotationConfiguration;
import net.nnwsf.configuration.AuthenticatedResourcePath;
import net.nnwsf.configuration.ConfigurationManager;
import net.nnwsf.configuration.ConfigurationNames;
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

        String hostname = (String)ConfigurationManager.get(ConfigurationNames.APPLICATION_HOSTNAME, String.class);
        int port = (Integer)ConfigurationManager.get(ConfigurationNames.APPLICATION_PORT, Integer.class);
        String resourcePath = (String)ConfigurationManager.get(ConfigurationNames.APPLICATION_RESOURCEPATH, String.class);

        if(serverConfiguration != null) {
            if(serverConfiguration.port() != Integer.MIN_VALUE) {
                port = serverConfiguration.port();
            }
            if(!"".equals(serverConfiguration.hostname())) {
                hostname = (String)ConfigurationManager.get(serverConfiguration.hostname(), String.class);
            }
            if(!"".equals(serverConfiguration.resourcePath())) {
                hostname = (String)ConfigurationManager.get(serverConfiguration.resourcePath(), String.class);
            }
        }

        AnnotationConfiguration annotationConfiguration = ReflectionHelper.findAnnotation(applicationClass, AnnotationConfiguration.class);

        ClassDiscovery.init(applicationClass.getClassLoader(), annotationConfiguration.value());

        ServiceManager.init(ClassDiscovery.getPackagesToScan());

        String providerClassName = ConfigurationManager.get(ConfigurationNames.DATASOURCE_PROVIDERCLASS, String.class);
        Class<? extends PersistenceProvider> providerClass = null;
        
        String jdbcDriverClassName = ConfigurationManager.get(ConfigurationNames.DATASOURCE_JDBCDRIVER, String.class);
                
        String jdbcUrl = ConfigurationManager.get(ConfigurationNames.DATASOURCE_JDBCURL, String.class);
        String user = ConfigurationManager.get(ConfigurationNames.DATASOURCE_USERNAME, String.class);
        String password = ConfigurationManager.get(ConfigurationNames.DATASOURCE_PASSWORD, String.class);
        
        Map<String, Object> datasourceProperties = ConfigurationManager.get(ConfigurationNames.DATASOURCE_PROPERTIES, Map.class);
        
        Datasource datasource = ReflectionHelper.findAnnotation(applicationClass, Datasource.class);
        
        if(datasource != null) {
            providerClass = datasource.providerClass();
            jdbcDriverClassName = ConfigurationManager.get(datasource.jdbcDriver(), String.class);
            user = ConfigurationManager.get(datasource.user(), String.class);
            password = ConfigurationManager.get(datasource.password(), String.class);
            if(datasource.properties() != null) {
                datasourceProperties = Arrays.asList(datasource.properties()).stream().collect(Collectors.toMap(p -> p.name(), p -> p.value()));
            }
        }
        
        if(providerClassName != null) {
            try {
                providerClass =  (Class<? extends PersistenceProvider>)Class.forName(providerClassName);
            } catch(ClassNotFoundException e) {
                throw new RuntimeException("Unable to find class for name " + providerClassName, e);
            }   
        }
        
        PersistenceManager.init(
            providerClass, 
            jdbcDriverClassName, 
            jdbcUrl,             
            user, 
            password,
            datasourceProperties);
            
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

}