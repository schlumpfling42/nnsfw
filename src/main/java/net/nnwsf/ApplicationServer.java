package net.nnwsf;

import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import net.nnwsf.authentication.AuthenticationMechanism;
import net.nnwsf.configuration.AnnotationConfiguration;
import net.nnwsf.configuration.AuthenticatedResourcePath;
import net.nnwsf.configuration.ServerConfiguration;
import net.nnwsf.controller.Controller;
import net.nnwsf.handler.HttpHandlerImpl;
import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.Reflection;

public class ApplicationServer {

    private final static Logger log = Logger.getLogger(ApplicationServer.class.getName());

    private static ApplicationServer instance;

    public static ApplicationServer start(Class<?> applicationClass) {
        instance = new ApplicationServer(applicationClass);
        return instance;
    }

    private final ServerConfiguration configuration;

    private ApplicationServer(Class<?> applicationClass) {
        AnnotationConfiguration annotationConfiguration = Reflection.getInstance().findAnnotation(applicationClass, AnnotationConfiguration.class);
        configuration = Reflection.getInstance().getConfiguration(applicationClass);
        Collection<String> authenticatedResourcePaths = Reflection.getInstance()
            .findAnnotations(applicationClass, AuthenticatedResourcePath.class)
            .stream().map(annotation -> annotation.value())
            .collect(Collectors.toList());
        HttpHandler httpHandler;
        try {
            Collection<Class<Object>> controllerClasses = ClassDiscovery.getInstance().discoverAnnotatedClasses(applicationClass.getClassLoader(), annotationConfiguration.value(), Object.class, Controller.class).get(Controller.class);
            Collection<Class<io.undertow.security.api.AuthenticationMechanism>> authenticationMechanimsClasses = ClassDiscovery.getInstance().discoverAnnotatedClasses(applicationClass.getClassLoader(), annotationConfiguration.value(), io.undertow.security.api.AuthenticationMechanism.class, AuthenticationMechanism.class).get(AuthenticationMechanism.class);
            httpHandler = new HttpHandlerImpl(applicationClass.getClassLoader(), configuration.getResourcePath(), authenticatedResourcePaths, controllerClasses, authenticationMechanimsClasses);
        } catch(Exception e) {
            throw new RuntimeException("Unable to discover annotated classes", e);
        }

        log.info("Starting server at " + configuration.getHostname() + " port " + configuration.getPort());
        Undertow server = Undertow.builder()
                .addHttpListener(configuration.getPort(), configuration.getHostname())
                .setHandler(httpHandler)
                .build();
        server.start();
    }

}