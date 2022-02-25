package net.nnwsf.handler;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.pac4j.core.config.Config;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.pac4j.vertx.context.session.VertxSessionStore;
import org.pac4j.vertx.handler.impl.CallbackHandler;
import org.pac4j.vertx.handler.impl.CallbackHandlerOptions;
import org.pac4j.vertx.handler.impl.SecurityHandler;
import org.pac4j.vertx.handler.impl.SecurityHandlerOptions;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import io.vertx.mutiny.ext.web.handler.SessionHandler;
import io.vertx.mutiny.ext.web.handler.StaticHandler;
import io.vertx.mutiny.ext.web.sstore.LocalSessionStore;
import net.nnwsf.application.annotation.ApiDocConfiguration;
import net.nnwsf.application.annotation.AuthenticationProviderConfiguration;
import net.nnwsf.authentication.OpenIdConfiguration;
import net.nnwsf.authentication.annotation.Authenticated;
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
			}
			if(pathProxies.containsKey(proxy.getPath())) {
				throw new RuntimeException("Duplicate path detected: method=" + proxy.getHttpMethod() + ", path=" + proxy.getPath());
			}
			pathProxies.put(proxy.getPath(), proxy);
		});

		NocodeProxyFactory nocodeProxyFactory = new NocodeProxyFactory();
		
		nocodeProxyFactory.getProxies().forEach(proxy -> {
			Map<String, EndpointProxy> pathProxies = proxies.get(proxy.getHttpMethod());
			if (pathProxies == null) {
				pathProxies = new HashMap<>();
				proxies.put(proxy.getHttpMethod(), pathProxies);
			}
			if(pathProxies.containsKey(proxy.getPath())) {
				throw new RuntimeException("Duplicate path detected: method=" + proxy.getHttpMethod() + ", path=" + proxy.getPath());
			}
			pathProxies.put(proxy.getPath(), proxy);
		});
		
		LocalSessionStore vertxSessionStore = LocalSessionStore.create(vertx);
		
		if(authenticationProviderConfiguration != null && authenticationProviderConfiguration.jsonFileName() != null) {
			VertxSessionStore sessionStore = new VertxSessionStore(vertxSessionStore.getDelegate());
			try {
				OpenIdConfiguration apiAuthConfig = new OpenIdConfiguration(
					authenticationProviderConfiguration.jsonFileName(),
					authenticationProviderConfiguration.openIdDiscoveryUri());

				SecurityHandlerOptions apiOptions = new SecurityHandlerOptions().setClients("headerclient");
				SecurityHandler securityHandler = new SecurityHandler(vertx.getDelegate(), sessionStore, apiAuthConfig.getApiConfig(), new Pac4jAuthProvider(), apiOptions) {
					protected void unexpectedFailure(final io.vertx.ext.web.RoutingContext context, Throwable failure) {
						context.fail(401, toTechnicalException(failure));
					}
				};

				proxies.keySet().forEach(httpMethod -> {
					Map<String, EndpointProxy> pathProxies = proxies.get(httpMethod);
					switch(httpMethod.toUpperCase()) {
						case "PUT":
							pathProxies.entrySet().forEach(anEntry -> {
								if(needsAuthentication(anEntry.getValue())) {
									router.put(anEntry.getKey())
										.handler(ctx -> securityHandler.handle(ctx.getDelegate()))
										.respond(routingContext -> handle(routingContext, anEntry.getValue()));
								} 
							});
							break;
						case "GET":
							pathProxies.entrySet().forEach(anEntry -> {
								if(needsAuthentication(anEntry.getValue())) {
									router.get(anEntry.getKey()).handler(ctx -> securityHandler.handle(ctx.getDelegate())).respond(routingContext -> handle(routingContext, sessionStore, anEntry.getValue()));
								}
							});
							break;
						case "POST":
							pathProxies.entrySet().forEach(anEntry -> {
								if(needsAuthentication(anEntry.getValue())) {
									router.post(anEntry.getKey()).handler(ctx -> securityHandler.handle(ctx.getDelegate())).respond(routingContext -> handle(routingContext, anEntry.getValue()));
								}
							});
							break;
						case "DELETE":
							pathProxies.entrySet().forEach(anEntry -> {
								if(needsAuthentication(anEntry.getValue())) {
									router.delete(anEntry.getKey()).handler(ctx -> securityHandler.handle(ctx.getDelegate())).respond(routingContext -> handle(routingContext, anEntry.getValue()));
								}
							});
							break;
					}
				});
				router.errorHandler(401, context -> context.response().setStatusCode(401).end());

				if(!authenticatedResourcePaths.isEmpty()) {
					try {
						OpenIdConfiguration webAuthConfig = new OpenIdConfiguration(
							authenticationProviderConfiguration.jsonFileName(),
							authenticationProviderConfiguration.openIdDiscoveryUri());
		
						Config controllerConfig = webAuthConfig.getControllerConfig();
		
						final CallbackHandlerOptions callbackHandlerOptions = new CallbackHandlerOptions()
							.setDefaultClient("headerclient")
							.setSaveInSession(true);
						final CallbackHandler callbackHandler = new CallbackHandler(vertx.getDelegate(), sessionStore, controllerConfig, callbackHandlerOptions);
						router.getDelegate().get(authenticationProviderConfiguration.callbackPath()).handler(callbackHandler);
		
						SecurityHandlerOptions webOptions = new SecurityHandlerOptions().setClients("oidcclient");
		
						SecurityHandler webSecurityHandler = new SecurityHandler(vertx.getDelegate(), sessionStore, controllerConfig, new Pac4jAuthProvider(), webOptions) {
							protected void unexpectedFailure(final io.vertx.ext.web.RoutingContext context, Throwable failure) {
								context.fail(401, toTechnicalException(failure));
							}
						};
		
						authenticatedResourcePaths.forEach(aPath -> {
							Router subRouter = Router.router(vertx);
							subRouter.get("/*").handler(ctx -> 
							webSecurityHandler.handle(ctx.getDelegate())
							).handler(StaticHandler
									.create("." + resourcePath + aPath)
									.setCachingEnabled(false)
									.setAllowRootFileSystemAccess(false));
							router.mountSubRouter(aPath, subRouter);
							});
					} catch (Exception e) {
						throw new RuntimeException("Unable to initialize open auth", e);
					}
				}

			} catch (Exception e) {
				throw new RuntimeException("Unable to initialize open auth", e);
			}
		}

		proxies.keySet().forEach(httpMethod -> {
			Map<String, EndpointProxy> pathProxies = proxies.get(httpMethod);
			switch(httpMethod.toUpperCase()) {
				case "PUT":
					pathProxies.entrySet().forEach(anEntry -> {
						if(!needsAuthentication(anEntry.getValue())) {
							router.put(anEntry.getKey()).respond(routingContext -> handle(routingContext, anEntry.getValue()));
						} 
					});
					break;
				case "GET":
					pathProxies.entrySet().forEach(anEntry -> {
						if(!needsAuthentication(anEntry.getValue())) {
							router.get(anEntry.getKey()).respond(routingContext -> handle(routingContext, anEntry.getValue()));
						}
					});
					break;
				case "POST":
					pathProxies.entrySet().forEach(anEntry -> {
						if(!needsAuthentication(anEntry.getValue())) {
							router.post(anEntry.getKey()).respond(routingContext -> handle(routingContext, anEntry.getValue()));
						}
					});
					break;
				case "DELETE":
					pathProxies.entrySet().forEach(anEntry -> {
						if(!needsAuthentication(anEntry.getValue())) {
							router.delete(anEntry.getKey()).respond(routingContext -> handle(routingContext, anEntry.getValue()));
						}
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
	}

	private Uni<?> handle(RoutingContext routingContext, VertxSessionStore sessionStore, EndpointProxy endpointProxy) {
		try{
			final ProfileManager profileManager = new VertxProfileManager(new VertxWebContext(routingContext.getDelegate(), sessionStore), (VertxSessionStore) sessionStore);
			return handle(routingContext, endpointProxy, profileManager.getProfile().get());
		} catch (Exception e) {
			log.log(Level.SEVERE, "Unable to complete the request {}", e.getMessage());
			routingContext.fail(500, e);
		}
		return Uni.createFrom().voidItem();
	}


	private Uni<?> handle(RoutingContext routingContext, EndpointProxy endpointProxy) {
		return handle(routingContext, endpointProxy, null);
	}

	private Uni<?> handle(RoutingContext routingContext, EndpointProxy endpointProxy, UserProfile userProfile) {
		try{
			return controllerHandler.handleRequest(routingContext, endpointProxy, userProfile);
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



	private boolean needsAuthentication(EndpointProxy proxy) {
		for (Annotation annotation : proxy.getAnnotations()) {
			if (annotation.annotationType().isAssignableFrom(Authenticated.class)) {
				return true;
			}
		}
		return false;
	}
}