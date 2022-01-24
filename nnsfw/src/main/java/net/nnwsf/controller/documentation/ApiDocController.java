package net.nnwsf.controller.documentation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map    ;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.nnwsf.controller.annotation.ContentType;
import net.nnwsf.controller.annotation.Controller;
import net.nnwsf.controller.annotation.Delete;
import net.nnwsf.controller.annotation.Get;
import net.nnwsf.controller.annotation.Post;
import net.nnwsf.controller.annotation.Put;
import net.nnwsf.controller.annotation.RequestParameter;
import net.nnwsf.controller.documentation.annotation.ApiDoc;
import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.ReflectionHelper;

@Controller("/api-doc")
public class ApiDocController {

    private static Logger logger = Logger.getLogger(ApiDocController.class.getSimpleName());


    private class ControllerDescription {
        private String description;
        private String className;
    }

    private class EndpointDocumentation {
        private String method;
        private String path;
        private String description;
        private List<String> parameters;
        private String returnValue;
    }

    private Map<ControllerDescription, List<EndpointDocumentation>> endpoints;
    
    private String apiDocHtml;

    public ApiDocController() {
        endpoints = new LinkedHashMap<>();
        try {
            Map<Controller, Class<Object>> discoverAnnotatedClasses = ClassDiscovery.discoverAnnotatedClasses(Object.class, Controller.class);
            discoverAnnotatedClasses.entrySet().forEach(aControllerEntry -> {
                Class<?> aControllerClass = aControllerEntry.getValue();
                ApiDoc aControllerApiDoc = ReflectionHelper.findAnnotation(aControllerClass, ApiDoc.class);
                if(aControllerApiDoc != null) {
                    String controllerPath = aControllerEntry.getKey().value();
                    ControllerDescription aControllerDescription = new ControllerDescription();
                    aControllerDescription.description = aControllerApiDoc.value();
                    aControllerDescription.className = aControllerClass.getSimpleName();
                    endpoints.put(aControllerDescription, findApiDocEndpoints(controllerPath, aControllerEntry.getValue(), Put.class, Get.class, Post.class, Delete.class));
                }
            });
            StringBuilder output = new StringBuilder();
            output.append("<html>");
            output.append("<header><title>API Documentation</title></header>");
            output.append("<body>");
            endpoints.entrySet().forEach(anEntry -> {
                if(anEntry.getValue() != null && anEntry.getValue().size() > 0) {
                    output.append("<div>");
                    output.append("<h2>");
                    output.append(anEntry.getKey().className);
                    output.append("</h2>");
                    output.append("<p>");
                    output.append(anEntry.getKey().description);
                    output.append("</p>");
                    output.append("<ul>");
                    anEntry.getValue().forEach(anEndpointDoc -> {
                        output.append("<li>");
                        output.append("<h3>");
                        output.append(anEndpointDoc.method);
                        output.append(": ");
                        output.append(anEndpointDoc.path);
                        output.append("</h3>"); 
                        output.append("<p>");
                        output.append(anEndpointDoc.description);
                        output.append("</p>");
                        if(anEndpointDoc.parameters != null && anEndpointDoc.parameters.size() > 0) {
                            output.append("<p>");
                            output.append("<h4>Query parameters:</h4>");
                            output.append("<ul>");
                            anEndpointDoc.parameters.forEach(aParameter -> {
                                output.append("<li>");
                                output.append(aParameter);
                                output.append("</li>");
                            });
                            output.append("</ul>");
                            output.append("</p>");
                        }
                        output.append("</li>");
                    });
                    output.append("</ul>");
                    output.append("</div>");
                }
            });
            output.append("</body>");
            output.append("</html>");
    
            apiDocHtml = output.toString();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to initialize API Documentation endpoint", e);
        }
    }

    private List<EndpointDocumentation> findApiDocEndpoints(String rootPath, Class<?> aControllerClass, Class<?>... annotationClasses) {
        List<EndpointDocumentation> endpointDocs = new ArrayList<>();
        Arrays.stream(annotationClasses).forEach(annotationClass -> {
            ReflectionHelper.findAnnotationMethods(aControllerClass, annotationClass).entrySet().forEach(anEndpointEntry -> {
                ApiDoc aMethodApiDoc = ReflectionHelper.findAnnotation(anEndpointEntry.getValue(), ApiDoc.class);
                if(aMethodApiDoc != null) {
                    Annotation methodAnnotation = anEndpointEntry.getKey();
                    Method method = anEndpointEntry.getValue();
                    EndpointDocumentation endpointDoc = new EndpointDocumentation();
                    endpointDoc.method = annotationClass.getSimpleName();
                    endpointDoc.path = (rootPath + ReflectionHelper.getValue(methodAnnotation, "value")).replace("//", "/");
                    endpointDoc.description = aMethodApiDoc.value();
                    Map<Annotation, Parameter> annotatedParameters = ReflectionHelper.findParameterAnnotations(method, RequestParameter.class);
                    endpointDoc.parameters = annotatedParameters.entrySet().stream().map(anEntry -> {
                        return ((RequestParameter)anEntry.getKey()).value() + ": " + anEntry.getValue().getType().getName();
                    }).collect(Collectors.toList());
                    endpointDocs.add(endpointDoc);
                }
            });
        });
        return endpointDocs;
    }



    @Get("/")
    @ContentType("text/html; charset=UTF-8")
    public String getApiDoc() {
        return apiDocHtml;
    }
}
