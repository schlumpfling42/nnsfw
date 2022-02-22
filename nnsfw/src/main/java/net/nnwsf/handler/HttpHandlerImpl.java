package net.nnwsf.handler;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.auth.oauth2.OAuth2Auth;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import io.vertx.mutiny.ext.web.handler.OAuth2AuthHandler;
import io.vertx.mutiny.ext.web.handler.SessionHandler;
import io.vertx.mutiny.ext.web.handler.StaticHandler;
import io.vertx.mutiny.ext.web.sstore.LocalSessionStore;
import net.nnwsf.application.annotation.ApiDocConfiguration;
import net.nnwsf.application.annotation.AuthenticationProviderConfiguration;
import net.nnwsf.controller.converter.ContentTypeConverter;
import net.nnwsf.exceptions.NotFoundException;
import net.nnwsf.handler.controller.ControllerProxyFactory;
import net.nnwsf.handler.nocode.NocodeProxyFactory;

public class HttpHandlerImpl {

	private final static Logger log = Logger.getLogger(HttpHandlerImpl.class.getName());

	private static HttpHandlerImpl instance;

	public static void init(
		String protocol,
		String hostname,
		int port,
		ClassLoader applicationClassLoader, 
		String resourcePath,
		Collection<String> authenticatedResourcePaths, 
		Collection<Class<Object>> controllerClasses,
		Collection<Class<ContentTypeConverter>> converterClasses,
		AuthenticationProviderConfiguration authenticationProviderConfiguration,
		ApiDocConfiguration apiDocConfiguration,
		Vertx vertx) {
		instance = new HttpHandlerImpl(
			protocol,
			hostname,
			port,
			applicationClassLoader, resourcePath, authenticatedResourcePaths, controllerClasses, converterClasses, authenticationProviderConfiguration, apiDocConfiguration, vertx);
	}
	
	public static Router getRouter() {
		return instance.router;
	}

	public static String getHostname() {
		return instance.hostname;
	}

	public static int getPort() {
		return instance.port;
	}

	private final Map<String, Map<String, EndpointProxy>> proxies = new HashMap<>();
	private final EndpointHandlerImpl controllerHandler;

	private final Router router;
	private final String hostname;
	private final int port;


	private HttpHandlerImpl(
		String protocol,
		String hostname,
		int port,
		ClassLoader applicationClassLoader, String resourcePath,
		Collection<String> authenticatedResourcePaths, 
		Collection<Class<Object>> controllerClasses,
		Collection<Class<ContentTypeConverter>> converterClasses,
		AuthenticationProviderConfiguration authenticationProviderConfiguration,
		ApiDocConfiguration apiDocConfiguration,
		Vertx vertx) {
		this.hostname = hostname;
		this.port = port;
		this.controllerHandler = new EndpointHandlerImpl(converterClasses);

		router = Router.router(vertx);

		router.route()
			.handler(SessionHandler.create(LocalSessionStore.create(vertx)));

		BodyHandler bodyHandler = BodyHandler.create();
		router.post().handler(bodyHandler::handle);
		router.put().handler(bodyHandler::handle);

		ControllerProxyFactory controllerProxyFactory = new ControllerProxyFactory(controllerClasses);
		
		controllerProxyFactory.getProxies().forEach(proxy -> {
			Map<String, EndpointProxy> pathProxies = proxies.get(proxy.getHttpMethod());
			if (pathProxies == null) {
				pathProxies = new HashMap<>();
				proxies.put(proxy.getHttpMethod(), pathProxies);
				if(pathProxies.containsKey(proxy.getPath())) {
					throw new RuntimeException("Duplicate path detected: method=" + proxy.getHttpMethod() + ", path=" + proxy.getPath());
				}
			}
			pathProxies.put(proxy.getPath(), proxy);
		});

		NocodeProxyFactory nocodeProxyFactory = new NocodeProxyFactory();
		
		nocodeProxyFactory.getProxies().forEach(proxy -> {
			Map<String, EndpointProxy> pathProxies = proxies.get(proxy.getHttpMethod());
			if (pathProxies == null) {
				pathProxies = new HashMap<>();
				proxies.put(proxy.getHttpMethod(), pathProxies);
				if(pathProxies.containsKey(proxy.getPath())) {
					throw new RuntimeException("Duplicate path detected: method=" + proxy.getHttpMethod() + ", path=" + proxy.getPath());
				}
			}
			pathProxies.put(proxy.getPath(), proxy);
		});

		proxies.keySet().forEach(httpMethod -> {
			Map<String, EndpointProxy> pathProxies = proxies.get(httpMethod);
			switch(httpMethod.toUpperCase()) {
				case "PUT":
					pathProxies.entrySet().forEach(anEntry -> {
						router.put(anEntry.getKey()).respond(routingContext -> handle(routingContext, anEntry.getValue()));
					});
					break;
				case "GET":
					pathProxies.entrySet().forEach(anEntry -> {
						router.get(anEntry.getKey()).respond(routingContext -> handle(routingContext, anEntry.getValue()));
					});
					break;
				case "POST":
					pathProxies.entrySet().forEach(anEntry -> {
						router.post(anEntry.getKey()).respond(routingContext -> handle(routingContext, anEntry.getValue()));
					});
					break;
				case "DELETE":
					pathProxies.entrySet().forEach(anEntry -> {
						router.delete(anEntry.getKey()).respond(routingContext -> handle(routingContext, anEntry.getValue()));
					});
					break;
			}
		});

		if(apiDocConfiguration != null) {
			List<EndpointProxy> proxies = new ArrayList<>(controllerProxyFactory.getProxies());
			proxies.addAll(nocodeProxyFactory.getProxies());
			ApiDocHandler apiDocHandler = new ApiDocHandler(proxies);
			String apiDocPath = apiDocConfiguration.value();
			router.get(apiDocPath).respond(routingContext -> apiDocHandler.handle(routingContext));
		}

		router.get("/*").handler(StaticHandler
			.create("." + resourcePath )
			.setCachingEnabled(false)
			.setAllowRootFileSystemAccess(false)
		);
		
		if(authenticationProviderConfiguration != null && authenticationProviderConfiguration.jsonFileName() != null) {
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> credentialsMap = (Map<String, Object>) new ObjectMapper().readValue(
						new InputStreamReader(ClassLoader.getSystemClassLoader().getResourceAsStream(authenticationProviderConfiguration.jsonFileName())),
						Map.class);
				@SuppressWarnings("unchecked")
				Map<String, Object> credentialParameters =  (Map<String, Object>) credentialsMap.get("web");

				String clientId = (String) credentialParameters.get("client_id");
				String clientSecret =(String) credentialParameters.get("client_secret");

				URL authUri = URI.create((String)credentialParameters.get("auth_uri")).toURL();
				// authUri
				String tokenUri = (String)credentialParameters.get("token_uri");


				OAuth2Auth authProvider = OAuth2Auth.create(vertx, new OAuth2Options()
					.setClientId(clientId)
					.setClientSecret(clientSecret)
					.setFlow(OAuth2FlowType.AUTH_CODE)
					.setSite(authUri.getProtocol() + "://" + authUri.getHost())
					.setTokenPath(tokenUri)
					.setAuthorizationPath(authUri.getPath()))
				;

				OAuth2AuthHandler handler = OAuth2AuthHandler.create(vertx, authProvider, protocol + "://" + hostname + ":" + port  + authenticationProviderConfiguration.callbackPath())
						.setupCallback(router.route(authenticationProviderConfiguration.callbackPath()));

				Field aField = handler.getClass().getDeclaredField("delegate");
				aField.setAccessible(true);
				aField.set(handler, handler.getDelegate().withScopes(Arrays.asList("https://www.googleapis.com/auth/userinfo.email", "openid", "https://www.googleapis.com/auth/userinfo.profile")));

				authenticatedResourcePaths.forEach(aPath -> {
					Router subRouter = Router.router(vertx);
					subRouter.get("/*").handler(handler)
						.handler(StaticHandler
							.create("." + resourcePath + aPath)
							.setCachingEnabled(false)
							.setAllowRootFileSystemAccess(false));
					router.mountSubRouter(aPath, subRouter);
					});
			} catch (Exception e) {
				throw new RuntimeException("Unable to initialize open auth", e);
			}
		}
	}


	private Uni<?> handle(RoutingContext routingContext, EndpointProxy endpointProxy) {
		try{
			return controllerHandler.handleRequest(routingContext, endpointProxy);
		} catch (IllegalArgumentException iae) {
			log.log(Level.SEVERE, "Unable to complete the request", iae);
			routingContext.fail(400, iae);
		} catch (NotFoundException nfe) {
			log.log(Level.SEVERE, "Unable to complete the request", nfe);
			routingContext.fail(404, nfe);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Unable to complete the request", e);
			routingContext.fail(500, e);
		} finally {
			log.log(Level.FINEST, "HttpRequest: end");
		}
		return routingContext.end();
	}



	// private boolean needsAuthentication(EndpointProxy proxy) {
	// 	for (Annotation annotation : proxy.getAnnotations()) {
	// 		if (annotation.annotationType().isAssignableFrom(Authenticated.class)) {
	// 			return true;
	// 		}
	// 	}
	// 	return false;
	// }

}