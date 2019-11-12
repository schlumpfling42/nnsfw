package net.nnwsf.handler;

import static net.nnwsf.handler.ControllerProxy.CONTROLLER_PROXY_ATTACHMENT_KEY;
import static net.nnwsf.handler.URLMatcher.URL_MATCHER_ATTACHMENT_KEY;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import net.nnwsf.authentication.Authenticated;
import net.nnwsf.authentication.IdentityManagerImplementation;
import net.nnwsf.controller.AuthenticationPrincipal;
import net.nnwsf.controller.Controller;
import net.nnwsf.controller.Get;
import net.nnwsf.controller.PathVariable;
import net.nnwsf.controller.Post;
import net.nnwsf.controller.RequestBody;
import net.nnwsf.controller.RequestParameter;
import net.nnwsf.util.Reflection;

public class HttpHandlerImpl implements HttpHandler {

	private final static Logger log = Logger.getLogger(HttpHandlerImpl.class.getName());

	private final Map<URLMatcher, Collection<ControllerProxy>> proxies = new HashMap<>();
	private final Map<URLMatcher, Map<Collection<String>, ControllerProxy>> matchedProxies = new HashMap<>();
	private final Collection<Class<Object>> controllerClasses;

	private final HttpHandler controllerSecurityHandler;
	private final HttpHandler controllerHandler;
	private final HttpHandler resourceSecurityHandler;
	private final HttpHandler resourceHandler;
	private final Collection<String> authenticatedResourcePaths;


	public HttpHandlerImpl(
			ClassLoader applicationClassLoader,
			String resourcePath,
			Collection<String> authenticatedResourcePaths,
			Collection<Class<Object>> controllerClasses,
			Collection<Class<AuthenticationMechanism>> authenticationMechanisms) {
		this.controllerClasses = controllerClasses;
		IdentityManager identityManager = new IdentityManagerImplementation();
		this.controllerHandler = new ControllerHandlerImpl();
		HttpHandler aControllerHandler = new AuthenticationCallHandler(controllerHandler);
		aControllerHandler = new AuthenticationConstraintHandler(aControllerHandler);
		List<AuthenticationMechanism> mechanisms = new ArrayList<>(Reflection.getInstance().getInstances(authenticationMechanisms));
		aControllerHandler = new AuthenticationMechanismsHandler(aControllerHandler, mechanisms);
		controllerSecurityHandler = new SecurityInitialHandler(
				AuthenticationMode.CONSTRAINT_DRIVEN, identityManager, aControllerHandler);

		String cleanedResourcePath = "";
		if(resourcePath.startsWith("/")) {
			cleanedResourcePath = resourcePath.substring(1);
		} else {
			cleanedResourcePath = resourcePath;
		}

		this.authenticatedResourcePaths = authenticatedResourcePaths;
		resourceHandler = new ResourceHandlerImpl(applicationClassLoader, cleanedResourcePath);
		HttpHandler aResourceHandler = new AuthenticationCallHandler(resourceHandler);
		aResourceHandler = new AuthenticationConstraintHandler(aResourceHandler);
		aResourceHandler = new AuthenticationMechanismsHandler(aResourceHandler, mechanisms);
		resourceSecurityHandler = new SecurityInitialHandler(
			AuthenticationMode.CONSTRAINT_DRIVEN, identityManager, aResourceHandler);
	}

	@Override
	public void handleRequest(final HttpServerExchange exchange) throws Exception {
		if (exchange.isInIoThread()) {
     		exchange.dispatch(this);
      		return;
    	}
		log.log(Level.INFO, "HttpRequest: start");
		HttpString method = exchange.getRequestMethod();

		URLMatcher requestUrlMatcher = new URLMatcher(method.toString(), exchange.getRequestPath());
		exchange.putAttachment(URL_MATCHER_ATTACHMENT_KEY, requestUrlMatcher);

		ControllerProxy proxy = findController(exchange, requestUrlMatcher);
		if(proxy != null) {
			exchange.putAttachment(CONTROLLER_PROXY_ATTACHMENT_KEY, proxy);
			if(needsAuthentication(proxy)) {
				controllerSecurityHandler.handleRequest(exchange);
			} else {
				controllerHandler.handleRequest(exchange);
			}
		} else {
			boolean authenticate = false;
			String requestPath = exchange.getRequestPath();
			for(String aPath : authenticatedResourcePaths) {
				if(requestPath.startsWith(aPath)) {
					authenticate = true;
					break;
				}
			}
			if(authenticate) {
				resourceSecurityHandler.handleRequest(exchange);
			} else {
				resourceHandler.handleRequest(exchange);
			}
		}
		log.log(Level.INFO, "HttpRequest: end");
	}

	private ControllerProxy findController(HttpServerExchange exchange, URLMatcher requestUrlMatcher) throws Exception {
		ControllerProxy matchingProxy = getBestMatch(exchange, requestUrlMatcher);

		if(matchingProxy == null) {

			Collection<ControllerProxy> matchingProxies = proxies.get(requestUrlMatcher);

			Collection<Class<?>> processedClasses = new HashSet<>();

			for (Class<?> aClass : controllerClasses) {
				Controller controllerAnnotation = Reflection.getInstance().findAnnotation(aClass, Controller.class);
				Collection<Annotation> annotations = Arrays.asList(aClass.getAnnotations());
				Map<Get, Collection<Method>> annotatedGetMethods = Reflection.getInstance().findAnnotationMethods(aClass, Get.class);
				Map<Post, Collection<Method>> annotatedPostMethods = Reflection.getInstance().findAnnotationMethods(aClass, Post.class);
				Object object = aClass.getDeclaredConstructor().newInstance();
				if (annotatedGetMethods == null) {
					throw new RuntimeException("Invalid controller");
				}
				for (Get methodAnnotation : annotatedGetMethods.keySet()) {
					for (Method annotatedMethod : annotatedGetMethods.get(methodAnnotation)) {
						if (annotatedMethod == null) {
							throw new RuntimeException("Invalid controller");
						}
						AnnotatedMethodParameter[] annotatedMethodParameters = getMethodParameters(annotatedMethod);
						MethodParameter[] specialMethodParameters = getSpecialMethodParameters(annotatedMethod);
						URLMatcher proxyUrlMatcher = new URLMatcher(
								"Get",
								(controllerAnnotation.value() + "/" + methodAnnotation.value()).replace("/+", "/"));

						matchingProxies = proxies.get(proxyUrlMatcher);
						if (matchingProxies == null) {
							matchingProxies = new ArrayList<>();
							proxies.put(proxyUrlMatcher, matchingProxies);
							matchedProxies.put(proxyUrlMatcher, new HashMap<>());
						}

						matchingProxies.add(new ControllerProxy(object, annotations, annotatedMethod, annotatedMethodParameters, specialMethodParameters, Arrays.asList(proxyUrlMatcher.getPathElements())));
					}
				}

				for (Post methodAnnotation : annotatedPostMethods.keySet()) {
					for (Method annotatedMethod : annotatedPostMethods.get(methodAnnotation)) {
						if (annotatedMethod == null) {
							throw new RuntimeException("Invalid controller");
						}
						AnnotatedMethodParameter[] annotatedMethodParameters = getMethodParameters(annotatedMethod);
						MethodParameter[] specialMethodParameters = getSpecialMethodParameters(annotatedMethod);
						URLMatcher proxyUrlMatcher = new URLMatcher(
								"Post",
								(controllerAnnotation.value() + "/" + methodAnnotation.value()).replace("/+", "/"));

						matchingProxies = proxies.get(proxyUrlMatcher);
						if (matchingProxies == null) {
							matchingProxies = new ArrayList<>();
							proxies.put(proxyUrlMatcher, matchingProxies);
							matchedProxies.put(proxyUrlMatcher, new HashMap<>());
						}

						matchingProxies.add(new ControllerProxy(object, annotations, annotatedMethod, annotatedMethodParameters, specialMethodParameters, Arrays.asList(proxyUrlMatcher.getPathElements())));
					}
				}

				processedClasses.add(aClass);

			}
			controllerClasses.removeAll(processedClasses);

			matchingProxy = getBestMatch(exchange, requestUrlMatcher);
		}

		return matchingProxy;
	}

	private ControllerProxy getBestMatch(HttpServerExchange exchange, URLMatcher requestUrlMatcher) {
		Collection<ControllerProxy> allMatchingProxies = proxies.get(requestUrlMatcher);
		if(allMatchingProxies == null) {
			return null;
		}
		Collection<String> requestParameters = new HashSet<>(exchange.getQueryParameters().keySet());
		ControllerProxy parameterMatchingProxy = matchedProxies.get(requestUrlMatcher).get(requestParameters);

		if(parameterMatchingProxy != null) {
			return parameterMatchingProxy;
		}

		for(ControllerProxy aProxy : allMatchingProxies) {
			Collection<String> methodParameters = Arrays.asList(aProxy.getAnnotatedMethodParameters())
					.stream()
					.filter(s -> s != null && s.getAnnotation().annotationType().isAssignableFrom(RequestParameter.class))
					.map(m -> m.getName())
					.filter(s -> s != null).collect(Collectors.toList());
			if(Objects.equals(methodParameters, requestParameters)) {
				matchedProxies.get(requestUrlMatcher).put(requestParameters, aProxy);
				return aProxy;
			}
		}

		for(ControllerProxy aProxy : allMatchingProxies) {
			Collection<String> methodParameters = Arrays.asList(aProxy.getAnnotatedMethodParameters())
					.stream()
					.filter(s -> s != null && s.getAnnotation().annotationType().isAssignableFrom(RequestParameter.class))
					.map(m -> m.getName())
					.filter(s -> s != null).collect(Collectors.toList());
			if(methodParameters.isEmpty()) {
				matchedProxies.get(requestUrlMatcher).put(requestParameters, aProxy);
				return aProxy;
			}
		}

		return null;

	}

	private AnnotatedMethodParameter[] getMethodParameters(Method annotatedMethod) {
		Annotation[][] parameterAnnotations = annotatedMethod.getParameterAnnotations();
		AnnotatedMethodParameter[] annotatedMethodParameters = new AnnotatedMethodParameter[parameterAnnotations.length];
		for(int i = 0; i< parameterAnnotations.length; i++) {
			if(parameterAnnotations[i] != null ) {
				for(Annotation aParameterAnnotation : parameterAnnotations[i]) {
					if(aParameterAnnotation.annotationType().isAssignableFrom(RequestParameter.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, ((RequestParameter)aParameterAnnotation).value(), annotatedMethod.getParameterTypes()[i]);
					} else if(aParameterAnnotation.annotationType().isAssignableFrom(PathVariable.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, ((PathVariable)aParameterAnnotation).value(), annotatedMethod.getParameterTypes()[i]);
					} else if(aParameterAnnotation.annotationType().isAssignableFrom(RequestBody.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, "body", annotatedMethod.getParameterTypes()[i]);
					} else if(annotatedMethod.getParameters()[i].getType().isAssignableFrom(HttpServletRequest.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, "request", annotatedMethod.getParameterTypes()[i]);
					} else if(annotatedMethod.getParameters()[i].getType().isAssignableFrom(HttpServletResponse.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, "response", annotatedMethod.getParameterTypes()[i]);
					}
				}
			}
		}
		return annotatedMethodParameters;
	}

	private MethodParameter[] getSpecialMethodParameters(Method annotatedMethod) {
		Annotation[][] parameterAnnotations = annotatedMethod.getParameterAnnotations();
		MethodParameter[] annotatedMethodParameters = new MethodParameter[parameterAnnotations.length];
		for(int i = 0; i< parameterAnnotations.length; i++) {
			if(parameterAnnotations[i] != null ) {
				for(Annotation aParameterAnnotation : parameterAnnotations[i]) {
					if(aParameterAnnotation.annotationType().isAssignableFrom(RequestBody.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, "body", annotatedMethod.getParameterTypes()[i]);
					} else if(aParameterAnnotation.annotationType().isAssignableFrom(AuthenticationPrincipal.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, "principle", annotatedMethod.getParameterTypes()[i]);
					}
				}
			}
		}
		Class<?>[] parameters = annotatedMethod.getParameterTypes();
		for(int i = 0; i< parameters.length; i++) {
				if(parameters[i].isAssignableFrom(HttpServerExchange.class)) {
					annotatedMethodParameters[i] = new MethodParameter("exchange", parameters[i]);
				}
		}
		return annotatedMethodParameters;
	}


	private boolean needsAuthentication(ControllerProxy proxy) {
		for(Annotation annotation : proxy.getAnnotations()) {
			if(annotation.annotationType().isAssignableFrom(Authenticated.class)) {
				return true;
			}
		}
		return false;
	}
}