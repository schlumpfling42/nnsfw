package net.nnwsf.configuration;

import java.util.Collection;
import java.util.HashSet;

public class ServerConfigurationImpl {

    private final Collection<Class> controllerClasses = new HashSet<>();
    
    public int getPort() {
        return 8080;
    }
    public String getHostname() {
        return "localhost";
    }

    public Collection<Class> getControllerClasses() {
        return controllerClasses;
    }
}