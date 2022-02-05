package net.nnwsf.handler;

import static net.nnwsf.handler.EndpointProxy.ENDPOINT_PROXY_ATTACHMENT_KEY;
import static net.nnwsf.handler.URLMatcher.URL_MATCHER_ATTACHMENT_KEY;

import java.lang.annotation.Annotation;
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

import org.pac4j.core.exception.TechnicalException;
import org.pac4j.undertow.handler.CallbackHandler;
import org.pac4j.undertow.handler.SecurityHandler;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import net.nnwsf.application.annotation.ApiDocConfiguration;
import net.nnwsf.application.annotation.AuthenticationProviderConfiguration;
import net.nnwsf.application.annotation.NocodeConfiguration;
import net.nnwsf.authentication.OpenIdConfiguration;
import net.nnwsf.authentication.annotation.Authenticated;
import net.nnwsf.controller.annotation.RequestParameter;
import net.nnwsf.controller.converter.ContentTypeConverter;
import net.nnwsf.handler.controller.ControllerProxyFactory;
import net.nnwsf.handler.nocode.NocodeProxyFactory;

public class HttpHandlerImpl implements HttpHandler {

	private final static Logger log = Logger.getLogger(HttpHandlerImpl.class.getName());

	private final Map<URLMatcher, Collection<EndpointProxy>> proxies = new HashMap<>();
	private final Map<URLMatcher, Map<Collection<String>, EndpointProxy>> matchedProxies = new HashMap<>();

	private final HttpHandler apiSecurityHandler;
	private final String callbackPath;
	private final HttpHandler controllerHandler;
	private final HttpHandler resourceSecurityHandler;
	private final HttpHandler resourceHandler;
	private final HttpHandler callbackHandler;
	private final HttpHandler apiDocHandler;
	private final String apiDocPath;
	private final Collection<String> authenticatedResourcePaths;

	public HttpHandlerImpl(ClassLoader applicationClassLoader, String resourcePath,
			Collection<String> authenticatedResourcePaths, 
			Collection<Class<Object>> controllerClasses,
			Collection<Class<ContentTypeConverter>> converterClasses,
			Collection<Class<AuthenticationMechanism>> authenticationMechanisms,
			AuthenticationProviderConfiguration authenticationProviderConfiguration,
			ApiDocConfiguration apiDocConfiguration,
			NocodeConfiguration nocodeConfiguration) {

		this.controllerHandler = new EndpointHandlerImpl(converterClasses);
		this.authenticatedResourcePaths = authenticatedResourcePaths;
		String cleanedResourcePath = "";
		if (resourcePath.startsWith("/")) {
			cleanedResourcePath = resourcePath.substring(1);
		} else {
			cleanedResourcePath = resourcePath;
		}
		resourceHandler = new ResourceHandlerImpl(applicationClassLoader, cleanedResourcePath);
		HttpHandler aCallbackHandler = null;
		if (authenticationProviderConfiguration == null || authenticationProviderConfiguration.jsonFileName() == null) {
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
		
		ControllerProxyFactory controllerProxyFactory = new ControllerProxyFactory(controllerClasses);
		
		controllerProxyFactory.getProxies().forEach(proxy -> {
			Collection<EndpointProxy> existingProxies = proxies.get(proxy.getUrlMatcher());
			if (existingProxies == null) {
				existingProxies = new ArrayList<>();
				proxies.put(proxy.getUrlMatcher(), existingProxies);
				matchedProxies.put(proxy.getUrlMatcher(), new HashMap<>());
			}
			
			existingProxies.add(proxy);
		});

		if(nocodeConfiguration != null) {
			NocodeProxyFactory nocodeProxyFactory = new NocodeProxyFactory(applicationClassLoader, Arrays.asList(nocodeConfiguration.schemas()), nocodeConfiguration.controllerPath());
			
			nocodeProxyFactory.getProxies().forEach(proxy -> {
				Collection<EndpointProxy> existingProxies = proxies.get(proxy.getUrlMatcher());
				if (existingProxies == null) {
					existingProxies = new ArrayList<>();
					proxies.put(proxy.getUrlMatcher(), existingProxies);
					matchedProxies.put(proxy.getUrlMatcher(), new HashMap<>());
				}
				
				existingProxies.add(proxy);
			});
		}

		if(apiDocConfiguration != null) {
			apiDocHandler = new ApiDocHandler(controllerProxyFactory.getProxies());
			apiDocPath = apiDocConfiguration.value();
		} else {
			apiDocHandler = null;
			apiDocPath = null;
		}
	}
	
	@Override
	public void handleRequest(final HttpServerExchange exchange) {
		try {
			if (exchange.isInIoThread()) {
				exchange.dispatch(this);
				return;
			}
			log.log(Level.FINEST, "HttpRequest: start: {0}: {1}",
					new Object[] { exchange.getRequestMethod(), exchange.getRequestPath() });
			HttpString method = exchange.getRequestMethod();

			if(apiDocPath != null && apiDocPath.equals(exchange.getRequestPath())) {
				apiDocHandler.handleRequest(exchange);
			} else {
				URLMatcher requestUrlMatcher = new URLMatcher(method.toString(), exchange.getRequestPath());
				exchange.putAttachment(URL_MATCHER_ATTACHMENT_KEY, requestUrlMatcher);

				EndpointProxy proxy = findController(exchange, requestUrlMatcher);
				if (proxy != null) {
					exchange.putAttachment(ENDPOINT_PROXY_ATTACHMENT_KEY, proxy);

					if (needsAuthentication(proxy)) {
						try {
							if (apiSecurityHandler == null) {
								log.log(Level.SEVERE, "No valid authentication provider configuration");
								exchange.setStatusCode(500).getResponseSender()
										.send("Invalid authentication configuration");
							} else {
								apiSecurityHandler.handleRequest(exchange);
							}
						} catch (TechnicalException te) {
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
							if (resourceSecurityHandler == null) {
								log.log(Level.SEVERE, "No valid authentication provider configuration");
								exchange.setStatusCode(500).getResponseSender()
										.send("Invalid authentication configuration");
							} else {
								resourceSecurityHandler.handleRequest(exchange);
							}
						} else {
							resourceHandler.handleRequest(exchange);
						}
					}
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Unable to complete the request", e);
			exchange.setStatusCode(500).getResponseSender().send("Unexpected error");
		} finally {
			log.log(Level.FINEST, "HttpRequest: end");
		}
	}

	private EndpointProxy findController(HttpServerExchange exchange, URLMatcher requestUrlMatcher) throws Exception {
		EndpointProxy matchingProxy = getBestMatch(exchange, requestUrlMatcher);

		return matchingProxy;
	}

	private EndpointProxy getBestMatch(HttpServerExchange exchange, URLMatcher requestUrlMatcher) {
		Collection<EndpointProxy> allMatchingProxies = proxies.get(requestUrlMatcher);
		if (allMatchingProxies == null) {
			return null;
		}
		Collection<String> requestParameters = new HashSet<>(exchange.getQueryParameters().keySet());
		EndpointProxy parameterMatchingProxy = matchedProxies.get(requestUrlMatcher).get(requestParameters);

		if (parameterMatchingProxy != null) {
			return parameterMatchingProxy;
		}

		for (EndpointProxy aProxy : allMatchingProxies) {
			Collection<String> methodParameters = Arrays.asList(aProxy.getParameters()).stream()
					.filter(s -> s != null
							&& s.getAnnotation().annotationType().isAssignableFrom(RequestParameter.class))
					.map(m -> m.getName()).filter(s -> s != null).collect(Collectors.toList());
			if (Objects.equals(new HashSet<>(methodParameters), new HashSet<>(requestParameters))) {
				matchedProxies.get(requestUrlMatcher).put(requestParameters, aProxy);
				return aProxy;
			}
		}

		for (EndpointProxy aProxy : allMatchingProxies) {
			Collection<String> methodParameters = Arrays.asList(aProxy.getParameters()).stream()
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

	private boolean needsAuthentication(EndpointProxy proxy) {
		for (Annotation annotation : proxy.getAnnotations()) {
			if (annotation.annotationType().isAssignableFrom(Authenticated.class)) {
				return true;
			}
		}
		return false;
	}

}