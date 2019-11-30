package net.nnwsf.handler;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ResourceHandlerImpl extends ResourceHandler {

	private final static Logger log = Logger.getLogger(ResourceHandlerImpl.class.getName());

	public ResourceHandlerImpl(ClassLoader classLoader, String prefix) {
	    super(new ClassPathResourceManager(classLoader, prefix));
	}

	@Override
	public void handleRequest(final HttpServerExchange exchange) throws Exception {
		log.log(Level.INFO, "Resource request: start");
        super.handleRequest(exchange);
		log.log(Level.INFO, "Resource request: end");
	}

}