package net.nnwsf.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import net.nnwsf.application.Constants;
import net.nnwsf.controller.annotation.ContentType;
import net.nnwsf.controller.annotation.Controller;
import net.nnwsf.controller.annotation.Delete;
import net.nnwsf.controller.annotation.Get;
import net.nnwsf.controller.annotation.Post;
import net.nnwsf.controller.annotation.Put;
import net.nnwsf.controller.annotation.RequestBody;
import net.nnwsf.controller.annotation.RequestParameter;
import net.nnwsf.controller.documentation.annotation.ApiDoc;
import net.nnwsf.controller.documentation.model.BeanClassDescription;
import net.nnwsf.controller.documentation.model.ClassDescription;
import net.nnwsf.controller.documentation.model.CollectionClassDescription;
import net.nnwsf.controller.documentation.model.ControllerDoc;
import net.nnwsf.controller.documentation.model.EndpointDoc;
import net.nnwsf.controller.documentation.model.MapClassDescription;
import net.nnwsf.controller.documentation.model.SimpleClassDescription;
import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;

public class ApiDocHandler1 implements HttpHandler {

	private final static Logger logger = Logger.getLogger(ApiDocHandler1.class.getName());

	private static List<String> methodSortOrder = List.of("Put", "Get", "Post", "Delete");

    static final Collection<Class<?>> simpleTypes = Set.of(String.class, Integer.class, Long.class, Double.class, Float.class, BigDecimal.class, BigInteger.class, int.class, long.class, float.class, double.class, Character.class, char.class, CharSequence.class);

    private static String HTML_TEMPLATE_NAME = "api_html.jte";

    private final List<ControllerDoc> controllers;
    private final TemplateEngine templateEngine;

	public ApiDocHandler1() {
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
                    endpointDoc.setPath((rootPath + ReflectionHelper.getValue(methodAnnotation, "value")).replace("/+", "/"));
                    endpointDoc.setDescription(aMethodApiDoc.value());
                    endpointDoc.setContentType(aMethodContentType == null ? "n/a" : aMethodContentType.value());
                    Map<Annotation, Parameter> annotatedParameters = ReflectionHelper.findParameterAnnotations(method, RequestParameter.class);
                    Parameter requestBodyParameter = ReflectionHelper.findParameter(method, RequestBody.class);
                    endpointDoc.setResponseBodyType(getClassDescription(method.getReturnType(), ReflectionHelper.getGenericTypes(method)));
                    endpointDoc.setParameters(annotatedParameters.entrySet().stream()
                        .collect(Collectors.toMap(anEntry ->  ((RequestParameter)anEntry.getKey()).value(), anEntry -> anEntry.getValue().getType().getName())));
                    endpointDoc.setRequestBodyType(requestBodyParameter == null ? null : getClassDescription(requestBodyParameter.getType()));
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

    private ClassDescription getClassDescription(Class<?> aClass) {
        if(aClass == null || void.class.equals(aClass)) {
            return null;
        }
        if(simpleTypes.contains(aClass)) {
            return SimpleClassDescription.of(aClass);
        }
        return BeanClassDescription.of(Arrays.stream(aClass.getDeclaredFields())
            .map(this::getClassDescription)
            .collect(Collectors.toMap(
                        Entry::getKey,
                        Entry::getValue)));
    }   
	
	private ClassDescription getClassDescription(Class<?> aClass, Class<?>... genericClasses) {
        if(aClass.isAssignableFrom(Collection.class)) {
            return CollectionClassDescription.of(getClassDescription(genericClasses[0]));
        } else if(aClass.isAssignableFrom(Map.class)) {
            return MapClassDescription.of(getClassDescription(genericClasses[0]), getClassDescription(genericClasses[1]));
        } else if(simpleTypes.contains(aClass)) {
            return SimpleClassDescription.of(aClass);
        } else {
            return getClassDescription(aClass);
        }
    }

    private Map.Entry<String, ClassDescription> getClassDescription(Field aField) {
        return Map.entry(aField.getName(), getClassDescription(aField.getType(), ReflectionHelper.getGenericTypes(aField)));
    }

	@Override
	public void handleRequest(final HttpServerExchange exchange) throws Exception {
		exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, Constants.CONTENT_TYPE_TEXT_HTML);
		TemplateOutput output = new StringOutput();
        templateEngine.render(HTML_TEMPLATE_NAME, controllers, output);
        exchange.getResponseSender().send(output.toString());
	}

}