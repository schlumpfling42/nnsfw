package net.nnwsf.application;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.server.session.SessionCookieConfig;
import net.nnwsf.application.annotation.AnnotationConfiguration;
import net.nnwsf.application.annotation.ApiDocConfiguration;
import net.nnwsf.application.annotation.AuthenticatedResourcePathConfiguration;
import net.nnwsf.application.annotation.AuthenticationProviderConfiguration;
import net.nnwsf.application.annotation.NocodeConfiguration;
import net.nnwsf.application.annotation.ServerConfiguration;
import net.nnwsf.configuration.ConfigurationManager;
import net.nnwsf.controller.annotation.Controller;
import net.nnwsf.controller.converter.ContentTypeConverter;
import net.nnwsf.controller.converter.annotation.Converter;
import net.nnwsf.handler.HttpHandlerImpl;
import net.nnwsf.nocode.NocodeManager;
import net.nnwsf.persistence.DatasourceManager;
import net.nnwsf.persistence.PersistenceManager;
import net.nnwsf.service.ServiceManager;
import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.ReflectionHelper;
import net.nnwsf.util.TypeUtil;

public class ApplicationServer {

    public static void main(String[] args) {
        try {
            if (args.length == 1) {
                start(Class.forName(args[0]));
            }
        } catch (Exception e) {

        }
        System.out.println("Usage: ApplicationServer <server class>");

        System.exit(-1);
    }

    private static final Logger log = Logger.getLogger(ApplicationServer.class.getName());

    private static ApplicationServer instance;

    public static ApplicationServer start(Class<?> applicationClass) {
        ConfigurationManager.init(applicationClass.getClassLoader());
        try {
            LogManager.getLogManager()
                    .readConfiguration(applicationClass.getClassLoader().getResourceAsStream("logging.properties"));
        } catch (Exception e) {
            System.err.println("Unable to read log configuration");
        }
        log.log(Level.INFO, "Starting server application {0}", applicationClass);
        instance = new ApplicationServer(applicationClass);
        return instance;
    }

    private ApplicationServer(Class<?> applicationClass) {

        ServerConfiguration serverConfiguration = ReflectionHelper.findAnnotation(applicationClass,
                ServerConfiguration.class);
        if (serverConfiguration == null) {
            throw new IllegalStateException("ServerConfiguration annotation required to start the server");
        }
        serverConfiguration = ConfigurationManager.apply(serverConfiguration);

        String hostname = serverConfiguration.hostname();
        int port = serverConfiguration.port();
        String resourcePath = serverConfiguration.resourcePath();

        AnnotationConfiguration annotationConfiguration = ReflectionHelper.findAnnotation(applicationClass,
                AnnotationConfiguration.class);
        if(annotationConfiguration != null) {
            ClassDiscovery.init(annotationConfiguration.value());
        } else {
            ClassDiscovery.init(applicationClass.getPackageName().split("\\.")[0]);
        }

        DatasourceManager.init();

        TypeUtil.init();

        initServices();

        AuthenticationProviderConfiguration authenticationProviderConfiguration = ReflectionHelper.findAnnotation(
            applicationClass,
            AuthenticationProviderConfiguration.class
        );

        ApiDocConfiguration apiDocConfiguration = ReflectionHelper.findAnnotation(
            applicationClass,
            ApiDocConfiguration.class
        );

        NocodeConfiguration nocodeConfiguration = ReflectionHelper.findAnnotation(
            applicationClass,
            NocodeConfiguration.class
        );

        NocodeManager.init(applicationClass.getClassLoader(), nocodeConfiguration);

        initPersistence();

        authenticationProviderConfiguration = ConfigurationManager.apply(authenticationProviderConfiguration);

        Collection<String> authenticatedResourcePaths = ReflectionHelper
                .findAnnotations(applicationClass, AuthenticatedResourcePathConfiguration.class).stream()
                .map(annotation -> annotation.value()).collect(Collectors.toList());
        HttpHandler httpHandler;
        try {
            Collection<Class<Object>> controllerClasses = ClassDiscovery
                    .discoverAnnotatedClasses(Object.class, Controller.class).values();
            Collection<Class<ContentTypeConverter>> contentTypeConverterClasses = ClassDiscovery
                    .discoverAnnotatedClasses(ContentTypeConverter.class, Converter.class).values();
            httpHandler = new HttpHandlerImpl(
                applicationClass.getClassLoader(), resourcePath,
                    authenticatedResourcePaths, controllerClasses, 
                    contentTypeConverterClasses,
                    Collections.emptyList(),
                    authenticationProviderConfiguration,
                    apiDocConfiguration);
        } catch (Exception e) {
            throw new RuntimeException("Unable to discover annotated classes", e);
        }

        log.info("Starting server at " + hostname + " port " + port);
        Undertow server = Undertow.builder().addHttpListener(port, hostname)
                .setHandler(new SessionAttachmentHandler(httpHandler, new InMemorySessionManager("SessionManager"),
                        new SessionCookieConfig()))
                .build();
        server.start();
    }

    private void initPersistence() {
        PersistenceManager.init();
    }

    private void initServices() {
        ServiceManager.init();
    }

}