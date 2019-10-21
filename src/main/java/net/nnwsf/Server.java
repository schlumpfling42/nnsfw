package net.nnwsf;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.undertow.Undertow;
import net.nnwsf.configuration.AnnotationConfiguration;
import net.nnwsf.handler.HttpHandlerImplementation;
import net.nnwsf.configuration.ServerConfigurationImpl;
import net.nnwsf.controller.Controller;
import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.Reflection;

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
        HttpHandlerImplementation httpHandler;
        try {
            Collection<Class<?>> controllerClasses = discoverAnnotatedClasses(applicationClass.getClassLoader(), annotationConfiguration.value(), Controller.class).get(Controller.class);
            httpHandler = new HttpHandlerImplementation(controllerClasses);
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

    private Map<Class<?>, Collection<Class<?>>> discoverAnnotatedClasses(ClassLoader classloader, String rootPackage, Class<?>... annotationClasses) throws Exception {
        Map<Class<?>, Collection<Class<?>>> allAnnotatedClasses = new HashMap<>();
        Collection<Package> packagesToScan = Arrays.stream(Package.getPackages()).filter(p -> p.getName().startsWith(rootPackage)).collect(Collectors.toList());
        for (Package aPackage : packagesToScan) {
            Collection<Class<?>> classes = ClassDiscovery.getInstance().getClassesForPackage(aPackage, classloader);
            for (Class<?> aClass : classes) {
                for (Class<?> annotationClass : annotationClasses) {
                    Annotation[] classAnnotations = aClass.getAnnotations();
                    for (Annotation aClassAnnotation : classAnnotations) {
                        if(aClassAnnotation.annotationType().isAssignableFrom(annotationClass)) {
                            Collection<Class<?>> classesForAnnotation = allAnnotatedClasses.get(annotationClass);
                            if(classesForAnnotation == null) {
                                classesForAnnotation = new HashSet<>();
                                allAnnotatedClasses.put(annotationClass, classesForAnnotation);
                            }
                            classesForAnnotation.add(aClass);
                        }
                    }
                }
            }
        }
        return allAnnotatedClasses;
    }
}