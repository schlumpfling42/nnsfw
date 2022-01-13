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
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.naming.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.pac4j.core.config.Config;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.undertow.handler.CallbackHandler;
import org.pac4j.undertow.handler.SecurityHandler;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import net.nnwsf.authentication.Authenticated;
import net.nnwsf.authentication.internal.OpenIdConfiguration;
import net.nnwsf.configuration.AuthenticationProviderConfiguration;
import net.nnwsf.controller.Controller;
import net.nnwsf.controller.Delete;
import net.nnwsf.controller.Get;
import net.nnwsf.controller.PathVariable;
import net.nnwsf.controller.Post;
import net.nnwsf.controller.Put;
import net.nnwsf.controller.RequestBody;
import net.nnwsf.controller.RequestParameter;
import net.nnwsf.controller.AuthenticatedUser;
import net.nnwsf.util.InjectionHelper;
import net.nnwsf.util.ReflectionHelper;

public class HttpHandlerImpl implements HttpHandler {

	private final static Logger log = Logger.getLogger(HttpHandlerImpl.class.getName());

	private final Map<URLMatcher, Collection<ControllerProxy>> proxies = new HashMap<>();
	private final Map<URLMatcher, Map<Collection<String>, ControllerProxy>> matchedProxies = new HashMap<>();
	private final Collection<Class<Object>> controllerClasses;

	private final HttpHandler apiSecurityHandler;
	private final String callbackPath;
	private final HttpHandler controllerHandler;
	private final HttpHandler resourceSecurityHandler;
	private final HttpHandler resourceHandler;
	private final HttpHandler callbackHandler;
	private final Collection<String> authenticatedResourcePaths;

	public HttpHandlerImpl(ClassLoader applicationClassLoader, String resourcePath,
			Collection<String> authenticatedResourcePaths, Collection<Class<Object>> controllerClasses,
			Collection<Class<AuthenticationMechanism>> authenticationMechanisms,
			AuthenticationProviderConfiguration authenticationProviderConfiguration) {
		this.controllerClasses = controllerClasses;
		this.controllerHandler = new ControllerHandlerImpl();
		this.authenticatedResourcePaths = authenticatedResourcePaths;
		String cleanedResourcePath = "";
		if (resourcePath.startsWith("/")) {
			cleanedResourcePath = resourcePath.substring(1);
		} else {
			cleanedResourcePath = resourcePath;
		}
		resourceHandler = new ResourceHandlerImpl(applicationClassLoader, cleanedResourcePath);
		HttpHandler aCallbackHandler = null;
		if(authenticationProviderConfiguration == null || authenticationProviderConfiguration.jsonFileName() == null) {
			this.apiSecurityHandler = null;
			this.resourceSecurityHandler = null;
			this.callbackHandler = null;
			this.callbackPath = null;
		} else {
			HttpHandler anApiSercurityHandler = null;
			HttpHandler aResourceSercurityHandler = null;
			try {
				OpenIdConfiguration authConfig = new OpenIdConfiguration(
					authenticationProviderConfiguration.jsonFileName(), 
					authenticationProviderConfiguration.openIdDiscoveryUri());
				aCallbackHandler = CallbackHandler.build(authConfig.getControllerConfig(), null, true);
				anApiSercurityHandler = SecurityHandler.build(controllerHandler,
						authConfig.getApiConfig());
				aResourceSercurityHandler = SecurityHandler.build(resourceHandler,
						authConfig.getControllerConfig());
			} catch (Throwable t) {
				anApiSercurityHandler = null;
				aResourceSercurityHandler = null;
			}
			this.apiSecurityHandler = anApiSercurityHandler;
			this.resourceSecurityHandler = aResourceSercurityHandler;
			this.callbackHandler = aCallbackHandler;
			this.callbackPath = authenticationProviderConfiguration.callbackPath();
		}

	}

	@Override
	public void handleRequest(final HttpServerExchange exchange) {
		try {
			if (exchange.isInIoThread()) {
				exchange.dispatch(this);
				return;
			}
			log.log(Level.INFO, "HttpRequest: start: {0}: {1}",
					new Object[] { exchange.getRequestMethod(), exchange.getRequestPath() });
			HttpString method = exchange.getRequestMethod();

			URLMatcher requestUrlMatcher = new URLMatcher(method.toString(), exchange.getRequestPath());
			exchange.putAttachment(URL_MATCHER_ATTACHMENT_KEY, requestUrlMatcher);

			ControllerProxy proxy = findController(exchange, requestUrlMatcher);
			if (proxy != null) {
				exchange.putAttachment(CONTROLLER_PROXY_ATTACHMENT_KEY, proxy);

				if (needsAuthentication(proxy)) {
					try {
						if(apiSecurityHandler == null) {
							log.log(Level.SEVERE, "No valid authentication provider configuration");
							exchange.setStatusCode(500).getResponseSender().send("Invalid authentication configuration");
						} else {
							apiSecurityHandler.handleRequest(exchange);
						}
					} catch(TechnicalException te) {
						log.log(Level.SEVERE, "Invalid auth token");
						exchange.setStatusCode(401).getResponseSender().send("Invalid auth token");
					}
				} else {
					controllerHandler.handleRequest(exchange);
				}
			} else {
				if (callbackPath != null && callbackPath.equals(exchange.getRequestPath()) && callbackHandler != null) {
					callbackHandler.handleRequest(exchange);
				} else {
					boolean authenticate = false;
					String requestPath = exchange.getRequestPath();
					for (String aPath : authenticatedResourcePaths) {
						if (requestPath.startsWith(aPath)) {
							authenticate = true;
							break;
						}
					}
					if (authenticate) {
						if(resourceSecurityHandler == null) {
							log.log(Level.SEVERE, "No valid authentication provider configuration");
							exchange.setStatusCode(500).getResponseSender().send("Invalid authentication configuration");
						} else {
							resourceSecurityHandler.handleRequest(exchange);
						}
					} else {
						resourceHandler.handleRequest(exchange);
					}
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Unable to complete the request", e);
			exchange.setStatusCode(500).getResponseSender().send("Unexpected error");
		} finally {
			log.log(Level.INFO, "HttpRequest: end");
		}
	}

	private ControllerProxy findController(HttpServerExchange exchange, URLMatcher requestUrlMatcher) throws Exception {
		ControllerProxy matchingProxy = getBestMatch(exchange, requestUrlMatcher);

		if (matchingProxy == null) {

			Collection<ControllerProxy> matchingProxies = proxies.get(requestUrlMatcher);

			Collection<Class<?>> processedClasses = new HashSet<>();

			for (Class<?> aClass : controllerClasses) {
				Controller controllerAnnotation = ReflectionHelper.findAnnotation(aClass, Controller.class);
				Collection<Annotation> annotations = Arrays.asList(aClass.getAnnotations());
				Map<Annotation, Method> annotatedMethods = ReflectionHelper.findAnnotationMethods(aClass, Get.class,
						Post.class, Put.class, Delete.class);
				Object object = createObject(aClass);
				if (annotatedMethods == null) {
					throw new RuntimeException("Invalid controller");
				}
				for (Annotation methodAnnotation : annotatedMethods.keySet()) {
					Method annotatedMethod = annotatedMethods.get(methodAnnotation);
					if (annotatedMethod == null) {
						throw new RuntimeException("Invalid controller");
					}
					AnnotatedMethodParameter[] annotatedMethodParameters = getMethodParameters(annotatedMethod);
					MethodParameter[] specialMethodParameters = getSpecialMethodParameters(annotatedMethod);
					URLMatcher proxyUrlMatcher = new URLMatcher(methodAnnotation.annotationType().getSimpleName(),
							(controllerAnnotation.value() + "/"
									+ ReflectionHelper.getValue(methodAnnotation, "value").replace("/+", "/")));

					matchingProxies = proxies.get(proxyUrlMatcher);
					if (matchingProxies == null) {
						matchingProxies = new ArrayList<>();
						proxies.put(proxyUrlMatcher, matchingProxies);
						matchedProxies.put(proxyUrlMatcher, new HashMap<>());
					}

					matchingProxies
							.add(new ControllerProxy(object, annotations, annotatedMethod, annotatedMethodParameters,
									specialMethodParameters, Arrays.asList(proxyUrlMatcher.getPathElements())));
				}

				processedClasses.add(aClass);

			}
			controllerClasses.removeAll(processedClasses);

			matchingProxy = getBestMatch(exchange, requestUrlMatcher);
		}

		return matchingProxy;
	}

	private Object createObject(Class<?> aClass) throws Exception {
		return InjectionHelper.getInjectable(aClass, null);
	}

	private ControllerProxy getBestMatch(HttpServerExchange exchange, URLMatcher requestUrlMatcher) {
		Collection<ControllerProxy> allMatchingProxies = proxies.get(requestUrlMatcher);
		if (allMatchingProxies == null) {
			return null;
		}
		Collection<String> requestParameters = new HashSet<>(exchange.getQueryParameters().keySet());
		ControllerProxy parameterMatchingProxy = matchedProxies.get(requestUrlMatcher).get(requestParameters);

		if (parameterMatchingProxy != null) {
			return parameterMatchingProxy;
		}

		for (ControllerProxy aProxy : allMatchingProxies) {
			Collection<String> methodParameters = Arrays.asList(aProxy.getAnnotatedMethodParameters()).stream()
					.filter(s -> s != null
							&& s.getAnnotation().annotationType().isAssignableFrom(RequestParameter.class))
					.map(m -> m.getName()).filter(s -> s != null).collect(Collectors.toList());
			if (Objects.equals(new HashSet<>(methodParameters), new HashSet<>(requestParameters))) {
				matchedProxies.get(requestUrlMatcher).put(requestParameters, aProxy);
				return aProxy;
			}
		}

		for (ControllerProxy aProxy : allMatchingProxies) {
			Collection<String> methodParameters = Arrays.asList(aProxy.getAnnotatedMethodParameters()).stream()
					.filter(s -> s != null
							&& s.getAnnotation().annotationType().isAssignableFrom(RequestParameter.class))
					.map(m -> m.getName()).filter(s -> s != null).collect(Collectors.toList());
			if (methodParameters.isEmpty()) {
				matchedProxies.get(requestUrlMatcher).put(requestParameters, aProxy);
				return aProxy;
			}
		}

		return null;

	}

	private AnnotatedMethodParameter[] getMethodParameters(Method annotatedMethod) {
		Annotation[][] parameterAnnotations = annotatedMethod.getParameterAnnotations();
		AnnotatedMethodParameter[] annotatedMethodParameters = new AnnotatedMethodParameter[parameterAnnotations.length];
		for (int i = 0; i < parameterAnnotations.length; i++) {
			if (parameterAnnotations[i] != null) {
				for (Annotation aParameterAnnotation : parameterAnnotations[i]) {
					if (aParameterAnnotation.annotationType().isAssignableFrom(RequestParameter.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation,
								((RequestParameter) aParameterAnnotation).value(),
								annotatedMethod.getParameterTypes()[i]);
					} else if (aParameterAnnotation.annotationType().isAssignableFrom(PathVariable.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation,
								((PathVariable) aParameterAnnotation).value(), annotatedMethod.getParameterTypes()[i]);
					} else if (aParameterAnnotation.annotationType().isAssignableFrom(RequestBody.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, "body",
								annotatedMethod.getParameterTypes()[i]);
					} else if (annotatedMethod.getParameters()[i].getType()
							.isAssignableFrom(HttpServletRequest.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, "request",
								annotatedMethod.getParameterTypes()[i]);
					} else if (annotatedMethod.getParameters()[i].getType()
							.isAssignableFrom(HttpServletResponse.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, "response",
								annotatedMethod.getParameterTypes()[i]);
					}
				}
			}
		}
		return annotatedMethodParameters;
	}

	private MethodParameter[] getSpecialMethodParameters(Method annotatedMethod) {
		Annotation[][] parameterAnnotations = annotatedMethod.getParameterAnnotations();
		MethodParameter[] annotatedMethodParameters = new MethodParameter[parameterAnnotations.length];
		for (int i = 0; i < parameterAnnotations.length; i++) {
			if (parameterAnnotations[i] != null) {
				for (Annotation aParameterAnnotation : parameterAnnotations[i]) {
					if (aParameterAnnotation.annotationType().isAssignableFrom(RequestBody.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, "body",
								annotatedMethod.getParameterTypes()[i]);
					} else if (aParameterAnnotation.annotationType().isAssignableFrom(AuthenticatedUser.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, "user",
								annotatedMethod.getParameterTypes()[i]);
					}
				}
			}
		}
		Class<?>[] parameters = annotatedMethod.getParameterTypes();
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].isAssignableFrom(HttpServerExchange.class)) {
				annotatedMethodParameters[i] = new MethodParameter("exchange", parameters[i]);
			}
		}
		return annotatedMethodParameters;
	}

	private boolean needsAuthentication(ControllerProxy proxy) {
		for (Annotation annotation : proxy.getAnnotations()) {
			if (annotation.annotationType().isAssignableFrom(Authenticated.class)) {
				return true;
			}
		}
		return false;
	}
}