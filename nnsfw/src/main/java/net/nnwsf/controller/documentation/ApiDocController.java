package net.nnwsf.controller.documentation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import net.nnwsf.controller.annotation.RequestBody;
import net.nnwsf.controller.annotation.RequestParameter;
import net.nnwsf.controller.documentation.annotation.ApiDoc;
import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.ReflectionHelper;
import net.nnwsf.util.ResourceUtil;
import net.nnwsf.util.TemplateUtil;

@Controller("/api-doc")
public class ApiDocController {

    private static Logger logger = Logger.getLogger(ApiDocController.class.getSimpleName());

    private static List<String> methodSortOrder = List.of("Put", "Get", "Post", "Delete");

    private class ControllerDescription {
        private String description;
        private String className;
    }

    private class EndpointDocumentation {
        private String method;
        private String path;
        private String description;
        private String contentType;
        private Map<String, String> parameters;
        private Class<?> requestBodyType;
        private Class<?> responseBodyType;
    }

    private static String HTML_TEMPLATE_NAME = "templates/api-doc/api_html.template";
    private static String CONTROLLER_TEMPLATE_NAME = "templates/api-doc/controller.template";
    private static String ENDPOINT_TEMPLATE_NAME = "templates/api-doc/endpoint.template";
    private static String REQUEST_TEMPLATE_NAME = "templates/api-doc/request.template";
    private static String RESPONSE_TEMPLATE_NAME = "templates/api-doc/response.template";
    private static String BODY_TEMPLATE_NAME = "templates/api-doc/body.template";
    private static String QUERY_PARAMETERS_TEMPLATE_NAME = "templates/api-doc/query_parameters.template";
    private static String PARAMETER_TEMPLATE_NAME = "templates/api-doc/parameter.template";

    private Map<ControllerDescription, List<EndpointDocumentation>> endpoints;
    
    private String apiDocHtml;

    private String htmlTemplate;
    private String controllerTemplate;
    private String endpointTemplate;
    private String requestTemplate;
    private String responseTemplate;
    private String bodyTemplate;
    private String queryParametersTemplate;
    private String parameterTemplate;

    public ApiDocController() {
        endpoints = new LinkedHashMap<>();
        try {
            htmlTemplate = ResourceUtil.getResourceAsString(this.getClass(), HTML_TEMPLATE_NAME);
            controllerTemplate = ResourceUtil.getResourceAsString(this.getClass(), CONTROLLER_TEMPLATE_NAME);
            endpointTemplate = ResourceUtil.getResourceAsString(this.getClass(), ENDPOINT_TEMPLATE_NAME);
            requestTemplate = ResourceUtil.getResourceAsString(this.getClass(), REQUEST_TEMPLATE_NAME);
            responseTemplate = ResourceUtil.getResourceAsString(this.getClass(), RESPONSE_TEMPLATE_NAME);
            bodyTemplate = ResourceUtil.getResourceAsString(this.getClass(), BODY_TEMPLATE_NAME);
            queryParametersTemplate = ResourceUtil.getResourceAsString(this.getClass(), QUERY_PARAMETERS_TEMPLATE_NAME);
            parameterTemplate = ResourceUtil.getResourceAsString(this.getClass(), PARAMETER_TEMPLATE_NAME);
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
            String body = endpoints.entrySet().stream()
                .filter(anEntry ->anEntry.getValue() != null && anEntry.getValue().size() > 0)
                .map(anEntry -> {
                    String endpoints = anEntry.getValue().stream().map(anEndpointDoc -> 
                        TemplateUtil.fill(
                            endpointTemplate, 
                            Map.of(
                                "method", anEndpointDoc.method, 
                                "path", anEndpointDoc.path, 
                                "description", anEndpointDoc.description,
                                "contentType", anEndpointDoc.contentType,
                                "request", fillRequestTemplate(anEndpointDoc),
                                "response", fillResponseTemplate(anEndpointDoc)
                            )
                        )
                    ).collect(Collectors.joining("\n"));

                    return TemplateUtil.fill(
                        controllerTemplate, 
                        Map.of("className", anEntry.getKey().className, "description", anEntry.getKey().description, "endpoints", endpoints));
                    }).collect(Collectors.joining("\n"));
                        
            apiDocHtml = TemplateUtil.fill(htmlTemplate, Map.of("body", body.toString()));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to initialize API Documentation endpoint", e);
        }
    }

    private String fillRequestTemplate(EndpointDocumentation anEndpointDoc) {
        return TemplateUtil.fill(
            requestTemplate, 
            Map.of(
                "queryParameters", 
                fillQueryParametersTemplate(anEndpointDoc),
                "body", 
                fillRequestBody(anEndpointDoc.requestBodyType)
            )
        );
    }

    private String fillResponseTemplate(EndpointDocumentation anEndpointDoc) {
        return TemplateUtil.fill(
            responseTemplate, 
            Map.of(
                "body", 
                fillResponseBody(anEndpointDoc.requestBodyType)
            )
        );
    }

    private String fillRequestBody(Class<?> aClass) {
        if(aClass == null) {
            return "";
        }
        return TemplateUtil.fill(bodyTemplate, Map.of("body", aClass.getName()));
    }

    private String fillResponseBody(Class<?> aClass) {
        if(aClass == null) {
            return "";
        }
        return TemplateUtil.fill(bodyTemplate, Map.of("body", aClass.getName()));
    }

    private String fillQueryParameters(Map<String, String> parameters) {
        return parameters.entrySet().stream().map(aParameter -> 
                        TemplateUtil.fill(parameterTemplate, Map.of("parameter", aParameter.getKey(), "type", aParameter.getValue()))
            ).collect(Collectors.joining("\n"));
    }

    private String fillQueryParametersTemplate(EndpointDocumentation anEndpointDoc) {
        if(anEndpointDoc.parameters == null || anEndpointDoc.parameters.isEmpty()) {
            return "";
        }
        return TemplateUtil.fill(
            queryParametersTemplate, 
            Map.of(
                "parameters", 
                fillQueryParameters(anEndpointDoc.parameters)
            )
        );
    }

    private List<EndpointDocumentation> findApiDocEndpoints(String rootPath, Class<?> aControllerClass, Class<?>... annotationClasses) {
        List<EndpointDocumentation> endpointDocs = new ArrayList<>();
        Arrays.stream(annotationClasses).forEach(annotationClass -> {
            ReflectionHelper.findAnnotationMethods(aControllerClass, annotationClass).entrySet().forEach(anEndpointEntry -> {
                ApiDoc aMethodApiDoc = ReflectionHelper.findAnnotation(anEndpointEntry.getValue(), ApiDoc.class);
                ContentType aMethodContentType = ReflectionHelper.findAnnotation(anEndpointEntry.getValue(), ContentType.class);
                if(aMethodApiDoc != null) {
                    Annotation methodAnnotation = anEndpointEntry.getKey();
                    Method method = anEndpointEntry.getValue();
                    EndpointDocumentation endpointDoc = new EndpointDocumentation();
                    endpointDoc.method = annotationClass.getSimpleName();
                    endpointDoc.path = (rootPath + ReflectionHelper.getValue(methodAnnotation, "value")).replace("//", "/");
                    endpointDoc.description = aMethodApiDoc.value();
                    endpointDoc.contentType = aMethodContentType == null ? "n/a" : aMethodContentType.value();
                    Map<Annotation, Parameter> annotatedParameters = ReflectionHelper.findParameterAnnotations(method, RequestParameter.class);
                    Parameter requestBodyParameter = ReflectionHelper.findParameter(method, RequestBody.class);
                    endpointDoc.responseBodyType = method.getReturnType();
                    endpointDoc.parameters = annotatedParameters.entrySet().stream()
                        .collect(Collectors.toMap(anEntry ->  ((RequestParameter)anEntry.getKey()).value(), anEntry -> anEntry.getValue().getType().getName()));
                    endpointDoc.requestBodyType = requestBodyParameter == null ? null : requestBodyParameter.getType();
                    endpointDocs.add(endpointDoc);
                }
            });
        });
        Collections.sort(endpointDocs, new Comparator<EndpointDocumentation>() {

            @Override
            public int compare(EndpointDocumentation o1, EndpointDocumentation o2) {
                int result = o1.path.compareTo(o2.path);
                if(result == 0) {
                    result = methodSortOrder.indexOf(o1.method) - methodSortOrder.indexOf(o2.method);
                }
                return result;
            }

        });
        return endpointDocs;
    }

    @Get("/")
    @ContentType("text/html; charset=UTF-8")
    public String getApiDoc() {
        return apiDocHtml;
    }
}
