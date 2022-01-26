package net.nnwsf.controller.documentation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map    ;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import gg.jte.CodeResolver;
import gg.jte.TemplateEngine;
import gg.jte.TemplateException;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.ResourceCodeResolver;
import net.nnwsf.controller.annotation.ContentType;
import net.nnwsf.controller.annotation.Controller;
import net.nnwsf.controller.annotation.Delete;
import net.nnwsf.controller.annotation.Get;
import net.nnwsf.controller.annotation.Post;
import net.nnwsf.controller.annotation.Put;
import net.nnwsf.controller.annotation.RequestBody;
import net.nnwsf.controller.annotation.RequestParameter;
import net.nnwsf.controller.documentation.annotation.ApiDoc;
import net.nnwsf.controller.documentation.model.ControllerDoc;
import net.nnwsf.controller.documentation.model.EndpointDoc;
import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.ReflectionHelper;

@Controller("/api-doc")
public class ApiDocController {

    private static Logger logger = Logger.getLogger(ApiDocController.class.getSimpleName());

    private static List<String> methodSortOrder = List.of("Put", "Get", "Post", "Delete");

    private static String HTML_TEMPLATE_NAME = "api_html.jte";

    private final List<ControllerDoc> controllers;
    private final TemplateEngine templateEngine;

    public ApiDocController() {
        controllers = new ArrayList<>();
        templateEngine = TemplateEngine.createPrecompiled(gg.jte.ContentType.Html);
        try {
            Map<Controller, Class<Object>> discoverAnnotatedClasses = ClassDiscovery.discoverAnnotatedClasses(Object.class, Controller.class);
            discoverAnnotatedClasses.entrySet().forEach(aControllerEntry -> {
                Class<?> aControllerClass = aControllerEntry.getValue();
                ApiDoc aControllerApiDoc = ReflectionHelper.findAnnotation(aControllerClass, ApiDoc.class);
                if(aControllerApiDoc != null) {
                    String controllerPath = aControllerEntry.getKey().value();
                    ControllerDoc aControllerDoc = new ControllerDoc();
                    aControllerDoc.setDescription(aControllerApiDoc.value());
                    aControllerDoc.setClassName(aControllerClass.getSimpleName());
                    aControllerDoc.setEndpoints(findApiDocEndpoints(controllerPath, aControllerEntry.getValue(), Put.class, Get.class, Post.class, Delete.class));
                    controllers.add(aControllerDoc);
                }
            });

        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to initialize API Documentation endpoint", e);
        }
    }

    private List<EndpointDoc> findApiDocEndpoints(String rootPath, Class<?> aControllerClass, Class<?>... annotationClasses) {
        List<EndpointDoc> endpointDocs = new ArrayList<>();
        Arrays.stream(annotationClasses).forEach(annotationClass -> {
            ReflectionHelper.findAnnotationMethods(aControllerClass, annotationClass).entrySet().forEach(anEndpointEntry -> {
                ApiDoc aMethodApiDoc = ReflectionHelper.findAnnotation(anEndpointEntry.getValue(), ApiDoc.class);
                ContentType aMethodContentType = ReflectionHelper.findAnnotation(anEndpointEntry.getValue(), ContentType.class);
                if(aMethodApiDoc != null) {
                    Annotation methodAnnotation = anEndpointEntry.getKey();
                    Method method = anEndpointEntry.getValue();
                    EndpointDoc endpointDoc = new EndpointDoc();
                    endpointDoc.setMethod(annotationClass.getSimpleName());
                    endpointDoc.setPath((rootPath + ReflectionHelper.getValue(methodAnnotation, "value")).replace("//", "/"));
                    endpointDoc.setDescription(aMethodApiDoc.value());
                    endpointDoc.setContentType(aMethodContentType == null ? "n/a" : aMethodContentType.value());
                    Map<Annotation, Parameter> annotatedParameters = ReflectionHelper.findParameterAnnotations(method, RequestParameter.class);
                    Parameter requestBodyParameter = ReflectionHelper.findParameter(method, RequestBody.class);
                    endpointDoc.setResponseBodyType(method.getReturnType());
                    endpointDoc.setParameters(annotatedParameters.entrySet().stream()
                        .collect(Collectors.toMap(anEntry ->  ((RequestParameter)anEntry.getKey()).value(), anEntry -> anEntry.getValue().getType().getName())));
                    endpointDoc.setRequestBodyType(requestBodyParameter == null ? null : requestBodyParameter.getType());
                    endpointDocs.add(endpointDoc);
                }
            });
        });
        Collections.sort(endpointDocs, new Comparator<EndpointDoc>() {

            @Override
            public int compare(EndpointDoc o1, EndpointDoc o2) {
                int result = o1.getPath().compareTo(o2.getPath());
                if(result == 0) {
                    result = methodSortOrder.indexOf(o1.getMethod()) - methodSortOrder.indexOf(o2.getMethod());
                }
                return result;
            }

        });
        return endpointDocs;
    }

    @Get("/")
    @ContentType("text/html; charset=UTF-8")
    public String getApiDoc() throws TemplateException {
        TemplateOutput output = new StringOutput();
        templateEngine.render(HTML_TEMPLATE_NAME, controllers, output);
        return output.toString();
    }
}
