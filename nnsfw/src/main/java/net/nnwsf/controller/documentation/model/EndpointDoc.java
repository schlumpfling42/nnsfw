package net.nnwsf.controller.documentation.model;

import java.util.Map;

public class EndpointDoc {
    
    private String method;
    private String path;
    private String description;
    private String contentType;
    private Map<String, String> parameters;
    private Class<?> requestBodyType;
    private Class<?> responseBodyType;

    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public Map<String, String> getParameters() {
        return parameters;
    }
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
    public Class<?> getRequestBodyType() {
        return requestBodyType;
    }
    public void setRequestBodyType(Class<?> requestBodyType) {
        this.requestBodyType = requestBodyType;
    }
    public Class<?> getResponseBodyType() {
        return responseBodyType;
    }
    public void setResponseBodyType(Class<?> responseBodyType) {
        this.responseBodyType = responseBodyType;
    }
}