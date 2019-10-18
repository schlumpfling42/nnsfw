package net.rka.server.fw;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import javax.management.relation.Relation;

import io.undertow.Undertow;
import net.rka.server.fw.handler.HttpHandlerImplementation;
import net.rka.server.fw.configuration.AnnotationConfiguration;
import net.rka.server.fw.configuration.ServerConfigurationImpl;
import net.rka.server.fw.controller.Controller;
import net.rka.server.fw.util.ClassDiscovery;
import net.rka.server.fw.util.Reflection;

public class Server {

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