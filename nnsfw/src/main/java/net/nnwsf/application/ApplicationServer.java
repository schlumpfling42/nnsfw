package net.nnwsf.application;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.tracing.opentracing.OpenTracingOptions;
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
import net.nnwsf.query.QueryParser;
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
        AnnotationConfiguration annotationConfiguration = ReflectionHelper.findAnnotation(applicationClass,
                AnnotationConfiguration.class);
        if (annotationConfiguration != null) {
            ClassDiscovery.init(annotationConfiguration.value());
        } else {
            ClassDiscovery.init(applicationClass.getPackageName().split("\\.")[0]);
        }

        DatasourceManager.init();

        TypeUtil.init();

        initServices();

        NocodeConfiguration nocodeConfiguration = ReflectionHelper.findAnnotation(
                applicationClass,
                NocodeConfiguration.class);

        NocodeManager.init(applicationClass.getClassLoader(), nocodeConfiguration);

        DeploymentOptions options = new DeploymentOptions()
            .setInstances(Math.min(4, Runtime.getRuntime().availableProcessors()));


        Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration.fromEnv()
            .withType(ConstSampler.TYPE)
            .withParam(1);
      
          Configuration.ReporterConfiguration reporterConfig = new Configuration.ReporterConfiguration()
            .withLogSpans(true);
      
          Configuration config = new Configuration("NNSFW")
            .withSampler(samplerConfig)
            .withReporter(reporterConfig);
        
        VertxOptions vertxOptions = new VertxOptions();

        vertxOptions.setTracingOptions(
            new OpenTracingOptions(config.getTracer())
        )
        .setPreferNativeTransport(true);

        Vertx vertx = Vertx.vertx(vertxOptions);

        ServerConfiguration serverConfiguration = ReflectionHelper.findAnnotation(applicationClass,
        ServerConfiguration.class);

        if (serverConfiguration == null) {
            throw new IllegalStateException("ServerConfiguration annotation required to start the server");
        }
        serverConfiguration = ConfigurationManager.apply(serverConfiguration);

        String protocol = serverConfiguration.protocol();
        String hostname = serverConfiguration.hostname();
        int port = serverConfiguration.port();
        String resourcePath = serverConfiguration.resourcePath();

        initPersistence(vertx);
        
        ApiDocConfiguration apiDocConfiguration = ReflectionHelper.findAnnotation(
                applicationClass,
                ApiDocConfiguration.class);


        QueryParser.init();
        AuthenticationProviderConfiguration authenticationProviderConfiguration = ReflectionHelper.findAnnotation(
                applicationClass,
                AuthenticationProviderConfiguration.class);
        authenticationProviderConfiguration = ConfigurationManager.apply(authenticationProviderConfiguration);
        Collection<String> authenticatedResourcePaths = ReflectionHelper
                .findAnnotations(applicationClass, AuthenticatedResourcePathConfiguration.class).stream()
                .map(annotation -> annotation.value()).collect(Collectors.toList());
                try {
            Collection<Class<Object>> controllerClasses = ClassDiscovery
                    .discoverAnnotatedClasses(Object.class, Controller.class).values();
            Collection<Class<ContentTypeConverter>> contentTypeConverterClasses = ClassDiscovery
                    .discoverAnnotatedClasses(ContentTypeConverter.class, Converter.class).values();
            HttpHandlerImpl.init(
                protocol,
                hostname,
                port,
                applicationClass.getClassLoader(), resourcePath,
                authenticatedResourcePaths, controllerClasses,
                contentTypeConverterClasses,
                authenticationProviderConfiguration,
                apiDocConfiguration,
                vertx);
        } catch (Exception e) {
            throw new RuntimeException("Unable to discover annotated classes", e);
        }
                

        vertx.deployVerticle(WebServerVerticle.class.getName(), options).subscribe().with( // <2>
                ok -> {
                    log.info("Deployment success");
                },
                err -> log.log(Level.SEVERE, "Deployment failure", err));
    }

    private void initPersistence(Vertx vertx) {
        PersistenceManager.init(vertx);
    }

    private void initServices() {
        ServiceManager.init();
    }


}