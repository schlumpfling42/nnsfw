package net.nnwsf.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import net.nnwsf.application.Constants;
import net.nnwsf.controller.annotation.RequestBody;
import net.nnwsf.controller.documentation.annotation.ApiDoc;
import net.nnwsf.controller.documentation.model.BeanClassDescription;
import net.nnwsf.controller.documentation.model.ClassDescription;
import net.nnwsf.controller.documentation.model.CollectionClassDescription;
import net.nnwsf.controller.documentation.model.ControllerDoc;
import net.nnwsf.controller.documentation.model.EndpointDoc;
import net.nnwsf.controller.documentation.model.MapClassDescription;
import net.nnwsf.controller.documentation.model.SimpleClassDescription;
import net.nnwsf.util.ReflectionHelper;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

public class ApiDocHandler implements HttpHandler {

	private final static Logger logger = Logger.getLogger(ApiDocHandler.class.getName());

	private static List<String> methodSortOrder = List.of("Put", "Get", "Post", "Delete");

    static final Collection<Class<?>> simpleTypes = Set.of(String.class, Integer.class, Long.class, Double.class, Float.class, BigDecimal.class, BigInteger.class, int.class, long.class, float.class, double.class, Character.class, char.class, CharSequence.class);

    private static String HTML_TEMPLATE_NAME = "api_html.jte";

    private final List<ControllerDoc> controllers;
    private final TemplateEngine templateEngine;

	public ApiDocHandler(Collection<EndpointProxy> proxies) {
        controllers = new ArrayList<>();
        templateEngine = TemplateEngine.createPrecompiled(gg.jte.ContentType.Html);
        try {
            Map<Class<?>, Collection<EndpointProxy>> proxyMap = new HashMap<>();
            proxies.forEach(aProxy -> {
                Collection<EndpointProxy> controllerProxies = proxyMap.get(aProxy.getControllerClass());
                if(controllerProxies == null) {
                    controllerProxies = new ArrayList<>();
                    proxyMap.put(aProxy.getControllerClass(), controllerProxies);
                }
                controllerProxies.add(aProxy);
            });
            proxyMap.entrySet().forEach(anEntry -> {
                ApiDoc aControllerApiDoc = ReflectionHelper.findAnnotation(anEntry.getKey(), ApiDoc.class);
                if(aControllerApiDoc != null) {
                    ControllerDoc aControllerDoc = new ControllerDoc();
                    aControllerDoc.setDescription(aControllerApiDoc.value());
                    aControllerDoc.setClassName(anEntry.getKey().getSimpleName());
                    List<EndpointDoc> endpoints = new ArrayList<>();
                    anEntry.getValue().forEach(aProxy -> {
                        aProxy.getAnnotations().stream().filter(anAnnotation -> ApiDoc.class.isInstance(anAnnotation)).findFirst().ifPresent(anAnnotation -> {
                            ApiDoc endpointApiDoc = (ApiDoc)anAnnotation;
                            EndpointDoc endpointDoc = new EndpointDoc();
                            endpointDoc.setMethod(aProxy.getHttpMethod());
                            endpointDoc.setPath(aProxy.getPath());
                            endpointDoc.setDescription(endpointApiDoc.value());
                            endpointDoc.setContentType(aProxy.getContentType() == null ? "n/a" : aProxy.getContentType());
                            MethodParameter requestBodyParameter = aProxy.getSpecialRequestParameter(RequestBody.class);
                            endpointDoc.setResponseBodyType(getClassDescription(aProxy.getReturnType(), aProxy.getGenericReturnTypes()));
                            endpointDoc.setParameters(Arrays.stream(aProxy.getParameters())
                                .filter(aParameter -> aParameter != null)
                                .collect(Collectors.toMap(aParam ->  aParam.getName(), aParam -> aParam.getType().getName())));
                            endpointDoc.setRequestBodyType(requestBodyParameter == null ? null : getClassDescription(requestBodyParameter.getType()));
                            endpoints.add(endpointDoc);
                        });
                    });
                    Collections.sort(endpoints, new Comparator<EndpointDoc>() {

                        @Override
                        public int compare(EndpointDoc o1, EndpointDoc o2) {
                            int result = o1.getPath().compareTo(o2.getPath());
                            if(result == 0) {
                                result = methodSortOrder.indexOf(o1.getMethod()) - methodSortOrder.indexOf(o2.getMethod());
                            }
                            return result;
                        }
            
                    });
                    aControllerDoc.setEndpoints(endpoints);
                    controllers.add(aControllerDoc);
                }
            });

        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to initialize API Documentation endpoint", e);
        }
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
        if(aClass == null) {
            return null;
        } else if(aClass.isAssignableFrom(Collection.class)) {
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