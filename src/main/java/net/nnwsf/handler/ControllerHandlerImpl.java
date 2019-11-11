package net.nnwsf.handler;

import static net.nnwsf.handler.ControllerProxy.CONTROLLER_PROXY_ATTACHMENT_KEY;
import static net.nnwsf.handler.URLMatcher.URL_MATCHER_ATTACHMENT_KEY;

import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import net.nnwsf.controller.AuthenticationPrincipal;
import net.nnwsf.controller.PathVariable;
import net.nnwsf.controller.RequestBody;
import net.nnwsf.controller.RequestParameter;

public class ControllerHandlerImpl implements HttpHandler {

	private final static Logger log = Logger.getLogger(ControllerHandlerImpl.class.getName());

	private final ObjectMapper mapper;

	public ControllerHandlerImpl() {
		this.mapper = new ObjectMapper();
	}

	@Override
	public void handleRequest(final HttpServerExchange exchange) throws Exception {
		log.log(Level.INFO, "Controller request: start");
		ControllerProxy controllerProxy = exchange.getAttachment(CONTROLLER_PROXY_ATTACHMENT_KEY);
		URLMatcher requestUrlMatcher = exchange.getAttachment(URL_MATCHER_ATTACHMENT_KEY);

		Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

		if(controllerProxy != null) {
			Object[] parameters = new Object[controllerProxy.getMethod().getParameterTypes().length];
			for(int i = 0; i<controllerProxy.getAnnotatedMethodParameters().length; i++) {
				AnnotatedMethodParameter annotatedMethodParameter = controllerProxy.getAnnotatedMethodParameters()[i];
				if (annotatedMethodParameter != null) {
					if (annotatedMethodParameter.getAnnotation().annotationType().isAssignableFrom(RequestParameter.class)) {
						Deque<String> parameterValue = queryParameters.get(annotatedMethodParameter.getName());
						if (parameterValue != null) {
							parameters[i] = parameterValue.element();
						}
					} else if (annotatedMethodParameter.getAnnotation().annotationType().isAssignableFrom(PathVariable.class)) {
						int index = controllerProxy.getPathElements().indexOf("{" + annotatedMethodParameter.getName() + "}");
						parameters[i] = requestUrlMatcher.getPathElements()[index];
					}
				}
			}
			for(int i = 0; i<controllerProxy.getSpecialMethodParameters().length; i++) {
				MethodParameter specialMethodParameter = controllerProxy.getSpecialMethodParameters()[i];
				if(specialMethodParameter != null) {
					if(specialMethodParameter instanceof AnnotatedMethodParameter) {
						AnnotatedMethodParameter annotatedSpecialMethodParameter = (AnnotatedMethodParameter)specialMethodParameter;
						if(annotatedSpecialMethodParameter.getAnnotation().annotationType().isAssignableFrom(RequestBody.class)) {
							StringBuilder body = new StringBuilder();
							exchange.startBlocking();
							try (InputStreamReader reader = new InputStreamReader(exchange.getInputStream(), Charset.defaultCharset())) {
								char[] buffer = new char[256];
								int read;
								while ((read = reader.read(buffer)) != -1) {
									body.append(buffer, 0, read);
								}
							}
							if (exchange.getRequestHeaders().get(Headers.CONTENT_TYPE).contains("application/json")) {
								parameters[i] = mapper.readValue(body.toString(), specialMethodParameter.getType());
							} else if (specialMethodParameter.getType().isAssignableFrom(String.class)) {
								parameters[i] = body.toString();
							}
						} else if(annotatedSpecialMethodParameter.getAnnotation().annotationType().isAssignableFrom(AuthenticationPrincipal.class)) {
							if(exchange.getSecurityContext() != null && exchange.getSecurityContext().getAuthenticatedAccount() != null) {
								parameters[i] = exchange.getSecurityContext().getAuthenticatedAccount().getPrincipal();
							}
						}
					} else if(specialMethodParameter.getType().isAssignableFrom(HttpServerExchange.class)) {
						parameters[i] = exchange;
					}
				}
			}
			Object result = controllerProxy.getMethod().invoke(controllerProxy.getInstance(), parameters);
			if(!exchange.isComplete()) {
				StringBuilder body = new StringBuilder();

				if (exchange.getRequestHeaders().get(Headers.ACCEPT).contains("application/json")) {
					body.append(mapper.writeValueAsString(result));
				} else if (result != null) {
					body.append(result.toString());
				}
				exchange.setStatusCode(200).getResponseSender().send(body.toString());
			}
		} else {
			log.log(Level.SEVERE, "Unable to find controller for " + exchange.getRequestPath() + exchange.getQueryString());
			exchange.setStatusCode(404);
		}
		log.log(Level.INFO, "Controller request: end");
	}

}