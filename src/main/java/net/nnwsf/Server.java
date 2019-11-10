package net.nnwsf;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import net.nnwsf.authentication.AuthenticationMechanism;
import net.nnwsf.configuration.AnnotationConfiguration;
import net.nnwsf.configuration.ServerConfiguration;
import net.nnwsf.configuration.ServerConfigurationImpl;
import net.nnwsf.controller.Controller;
import net.nnwsf.handler.HttpHandlerImpl;
import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.Reflection;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class Server {

    private final static Logger log = Logger.getLogger(Server.class.getName());

    private static Server instance;

    public static Server start(Class<?> applicationClass) {
        instance = new Server(applicationClass);
        return instance;
    }

    private final ServerConfigurationImpl configuration;

    private Server(Class<?> applicationClass) {
        AnnotationConfiguration annotationConfiguration = Reflection.getInstance().findAnnotation(applicationClass, AnnotationConfiguration.class);
        ServerConfiguration serverConfiguration = Reflection.getInstance().findAnnotation(applicationClass, ServerConfiguration.class);
        HttpHandler httpHandler;
        try {
            Collection<Class<Object>> controllerClasses = ClassDiscovery.getInstance().discoverAnnotatedClasses(applicationClass.getClassLoader(), annotationConfiguration.value(), Object.class, Controller.class).get(Controller.class);
            Collection<Class<io.undertow.security.api.AuthenticationMechanism>> authenticationMechanimsClasses = ClassDiscovery.getInstance().discoverAnnotatedClasses(applicationClass.getClassLoader(), annotationConfiguration.value(), io.undertow.security.api.AuthenticationMechanism.class, AuthenticationMechanism.class).get(AuthenticationMechanism.class);
            httpHandler = new HttpHandlerImpl(applicationClass.getClassLoader(), serverConfiguration.resourcePath(), controllerClasses, authenticationMechanimsClasses);
        } catch(Exception e) {
            throw new RuntimeException("Unable to discover annotated classes", e);
        }
        configuration = Reflection.getInstance().getConfiguration(applicationClass);
        log.info("Starting server at " + configuration.getHostname() + " port " + configuration.getPort());
        Undertow server = Undertow.builder()
                .addHttpListener(configuration.getPort(), configuration.getHostname())
                .setHandler(httpHandler)
                .build();
        server.start();
    }

    public ServerConfigurationImpl getConfiguration() {
        return configuration;
    }

}