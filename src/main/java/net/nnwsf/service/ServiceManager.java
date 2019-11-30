package net.nnwsf.service;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.nnwsf.util.ClassDiscovery;

public class ServiceManager {

    private static Logger log = Logger.getLogger(ServiceManager.class.getName());

    private static ServiceManager instance;

    public static void init(Collection<Package> packagesToScan) {
        if(instance == null) {
            try {
                Map<Service, Class<Object>> serviceClasses = ClassDiscovery.discoverAnnotatedClasses(Object.class, Service.class);
                instance = new ServiceManager(serviceClasses, packagesToScan);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Unable to discover services", e);
                throw new RuntimeException("Unable to discover services", e);
            }
        }
    }

    private final Collection<Class<?>> serviceClasses;
    private final Map<String, Class<?>> serviceImplementation;

    private final Map<String, Object> serviceSingletons;

    private ServiceManager(Map<Service, Class<Object>> annotationServiceClasses, Collection<Package> packagesToScan) {
        this.serviceClasses = new HashSet<>();
        this.serviceImplementation = new HashMap<>();
        for(Map.Entry<Service, Class<Object>> annotationClass : annotationServiceClasses.entrySet()) {
            Class<?> aClass = annotationClass.getValue();
            if(!annotationClass.getValue().isInterface()) {
                Class<?> anImplementationClass = annotationClass.getValue();
                Collection<Class<?>> allClasses = ClassDiscovery.getAllClassesAndInterfaces(anImplementationClass, packagesToScan);
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
        T service = (T)serviceSingletons.get(serviceName);
        if(service == null) {
            try {
                service = (T) implementationClass.newInstance();
                service = (T) Proxy.newProxyInstance(serviceClass.getClassLoader(), new Class<?>[] { serviceClass },
                        new ServiceInvocationHandler(service));
                serviceSingletons.put(serviceName, service);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Unable to create service {0} with implementation {1}", new Object[]{serviceClass, implementationClass});
                throw new RuntimeException(e);
            }
        }
        return service;
    }

	public static Object getActualServiceObject(Object injectable) {
        ServiceInvocationHandler handler = (ServiceInvocationHandler)Proxy.getInvocationHandler(injectable);
		return handler.getServiceObject();
	}

}

