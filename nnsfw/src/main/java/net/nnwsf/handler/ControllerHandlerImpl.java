package net.nnwsf.handler;

import static net.nnwsf.handler.ControllerProxy.CONTROLLER_PROXY_ATTACHMENT_KEY;
import static net.nnwsf.handler.URLMatcher.URL_MATCHER_ATTACHMENT_KEY;

import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.pac4j.undertow.account.Pac4jAccount;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import net.nnwsf.controller.annotation.AuthenticatedUser;
import net.nnwsf.controller.annotation.PathVariable;
import net.nnwsf.controller.annotation.RequestBody;
import net.nnwsf.controller.annotation.RequestParameter;
import net.nnwsf.handler.converter.ContentTypeConverter;
import net.nnwsf.handler.converter.JsonContentTypeConverter;
import net.nnwsf.handler.converter.TextContentTypeConverter;
import net.nnwsf.util.TypeUtil;
import net.nnwsf.authentication.annotation.User;

public class ControllerHandlerImpl implements HttpHandler {

	private final static Logger log = Logger.getLogger(ControllerHandlerImpl.class.getName());

	private final ObjectMapper mapper;

	private final Map<String, ContentTypeConverter> contentTypeConverters;
	private final ContentTypeConverter defaultContentTypeConverter = new TextContentTypeConverter();

	public ControllerHandlerImpl() {
		this.mapper = new ObjectMapper();
		this.contentTypeConverters = Map.of("application/json", new JsonContentTypeConverter());
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
							String value = parameterValue.element();
							parameters[i] = TypeUtil.toType(value, annotatedMethodParameter.getType());
						}
					} else if (annotatedMethodParameter.getAnnotation().annotationType().isAssignableFrom(PathVariable.class)) {
						int index = controllerProxy.getPathElements().indexOf("{" + annotatedMethodParameter.getName() + "}");
						String value = requestUrlMatcher.getPathElements()[index];
						parameters[i] = TypeUtil.toType(value, annotatedMethodParameter.getType());
					}
				}
			}
			ContentTypeConverter contentTypeConverter = getContentTypeConverter(controllerProxy.getContentType());

			for(int i = 0; i<controllerProxy.getSpecialMethodParameters().length; i++) {
				MethodParameter specialMethodParameter = controllerProxy.getSpecialMethodParameters()[i];
				if(specialMethodParameter != null) {
					if(specialMethodParameter instanceof AnnotatedMethodParameter) {
						AnnotatedMethodParameter annotatedSpecialMethodParameter = (AnnotatedMethodParameter)specialMethodParameter;
						if(annotatedSpecialMethodParameter.getAnnotation().annotationType().isAssignableFrom(RequestBody.class)) {
							exchange.startBlocking();
							parameters[i] = contentTypeConverter.readFrom(exchange.getInputStream(), specialMethodParameter.getType());
						} else if(annotatedSpecialMethodParameter.getAnnotation().annotationType().isAssignableFrom(AuthenticatedUser.class)) {
							if(exchange.getSecurityContext() != null && exchange.getSecurityContext().getAuthenticatedAccount() != null) {
								parameters[i] = new User((Pac4jAccount)exchange.getSecurityContext().getAuthenticatedAccount());
							}
						}
					} else if(specialMethodParameter.getType().isAssignableFrom(HttpServerExchange.class)) {
						parameters[i] = exchange;
					}
				}
			}
			try {
				Object result = controllerProxy.getMethod().invoke(controllerProxy.getInstance(), parameters);
				if(!exchange.isComplete()) {
					if(result == null) {
						exchange.setStatusCode(200).getResponseSender().send("");
					} else {
						exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, controllerProxy.getContentType());
						exchange.setStatusCode(200);
						exchange.startBlocking();
						contentTypeConverter.writeTo(result, exchange.getOutputStream());
					}
				}
			} catch(InvocationTargetException ite) {
				if(ite.getCause() != null) {
					throw (Exception)ite.getCause();
				}
				throw ite;
			}
		} else {
			log.log(Level.SEVERE, "Unable to find controller for " + exchange.getRequestPath() + exchange.getQueryString());
			exchange.setStatusCode(404);
		}
		log.log(Level.INFO, "Controller request: end");
	}

	private ContentTypeConverter getContentTypeConverter(String contentType) {
		ContentTypeConverter converter = contentTypeConverters.get(contentType);
		if(converter == null) {
			converter = defaultContentTypeConverter;
		}
		return converter;
	}
}