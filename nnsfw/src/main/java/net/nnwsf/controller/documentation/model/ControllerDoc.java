package net.nnwsf.controller.documentation.model;

import java.util.List;

public class ControllerDoc {
    
    private String description;
    private String className;
    private List<EndpointDoc> endpoints;

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
    public List<EndpointDoc> getEndpoints() {
        return endpoints;
    }
    public void setEndpoints(List<EndpointDoc> endpoints) {
        this.endpoints = endpoints;
    }

}

