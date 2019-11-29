package net.nnwsf.service;

import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

public class ServiceManager {

    private static Logger log = Logger.getLogger(ServiceManager.class.getName());

    private static ServiceManager instance;

    public static ServiceManager getInstance() {
        if(instance == null) {
            try {
                Map<Service, Class<Object>> serviceClasses = ClassDiscovery.getInstance().discoverAnnotatedClasses(Object.class, Service.class);
                instance = new ServiceManager(serviceClasses);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Unable to discover services", e);
                throw new RuntimeException("Unable to discover services", e);
            }
        }
        return instance;
    }

    private final Map<Class<?>, Service> serviceClasses;
    private final Map<String, Class<?>> serviceImplementation;

    private final Map<String, Object> serviceSingletons;

    public ServiceManager(Map<Service, Class<Object>> annotationServiceClasses) {
        this.serviceClasses = new HashMap<>();
        this.serviceImplementation = new HashMap<>();
        for(Map.Entry<Service, Class<Object>> annotationClass : annotationServiceClasses.entrySet()) {
            Class<?> aClass = annotationClass.getValue();
            Class<?> anImplementationClass = ClassDiscovery.getInstance().getImplementation(annotationClass.getValue());
            String serviceName = aClass + ":" + annotationClass.getKey().value();
            if(!anImplementationClass.isInterface()) {
                if (serviceImplementation.containsKey(serviceName)) {
                    throw new RuntimeException("Duplicate implementation for service: " + serviceName);
                } else {
                    serviceImplementation.put(serviceName, anImplementationClass);
                }
            }
            this.serviceClasses.put(aClass, annotationClass.getKey());
        }
        this.serviceSingletons = new HashMap<>();
    }

    public boolean isService(Class<?> serviceClass) {
        return serviceClasses.get(serviceClass) != null;
    }

    public <T> T createService(Class<T> serviceClass, String name) {
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

	public Object getActualServiceObject(Object injectable) {
        ServiceInvocationHandler handler = (ServiceInvocationHandler)Proxy.getInvocationHandler(injectable);
		return handler.getServiceObject();
	}

}

