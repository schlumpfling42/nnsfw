package net.nnwsf.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.nnwsf.service.annotation.Service;
import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.ProxyUtil;
import net.nnwsf.util.ReflectionHelper;

public class ServiceManager {

    private static Logger log = Logger.getLogger(ServiceManager.class.getName());

    private static ServiceManager instance;

    public static void init() {
        if(instance == null) {
            try {
                Map<Service, Class<Object>> serviceClasses = ClassDiscovery.discoverAnnotatedClasses(Service.class);
                instance = new ServiceManager(serviceClasses);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Unable to discover services", e);
                throw new RuntimeException("Unable to discover services", e);
            }
        }
    }

    private final Collection<Class<?>> serviceClasses;
    private final Map<String, Class<?>> serviceImplementation;

    private final Map<String, Object> serviceSingletons;

    private ServiceManager(Map<Service, Class<Object>> annotationServiceClasses) {
        this.serviceClasses = new HashSet<>();
        this.serviceImplementation = new HashMap<>();
        for(Map.Entry<Service, Class<Object>> annotationClass : annotationServiceClasses.entrySet()) {
            Class<?> anImplementationClass = annotationClass.getValue();
            if(anImplementationClass.isInterface()) {
                anImplementationClass = ClassDiscovery.getImplementation(anImplementationClass, Service.class, annotationClass.getKey().value());
            }
            Collection<Class<?>> allClasses = ReflectionHelper.getAllClassesAndInterfaces(anImplementationClass);
            for(Class<?> aSubClass : allClasses) {
                String serviceName = aSubClass + ":";
                if(!aSubClass.equals(anImplementationClass)) {
                    serviceName +=  annotationClass.getKey().value();
                }
                if(!anImplementationClass.isInterface()) {
                    if (serviceImplementation.containsKey(serviceName)) {
                        throw new RuntimeException("Duplicate implementation for service: " + serviceName);
                    } else {
                        serviceImplementation.put(serviceName, anImplementationClass);
                    }
                }
                this.serviceClasses.add(aSubClass);
            }
        }
        this.serviceSingletons = new HashMap<>();
    }

    public static boolean isService(Class<?> serviceClass) {
        return instance.serviceClasses.contains(serviceClass);
    }

    public static <T> T createService(Class<T> serviceClass, String name) {
        return instance.internalCreateService(serviceClass, name);
    }

    private <T> T internalCreateService(Class<T> serviceClass, String name) {
        String serviceName = serviceClass + ":" + (name == null ? "" : name);
        Class<?> implementationClass = serviceImplementation.get(serviceName);
        T service = serviceClass.cast(serviceSingletons.get(serviceName));
        if(service == null) {
            try {
                ServiceInterceptor serviceInterceptor = new ServiceInterceptor(implementationClass);
                service = serviceClass.cast(ProxyUtil.createProxy(implementationClass, serviceInterceptor));
                serviceSingletons.put(serviceName, service);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Unable to create service {0} with implementation {1}", new Object[]{serviceClass, implementationClass});
                throw new RuntimeException(e);
            }
        }
        return service;
    }

}

