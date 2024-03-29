package net.nnwsf.handler;

import static net.nnwsf.handler.EndpointProxy.ENDPOINT_PROXY_ATTACHMENT_KEY;
import static net.nnwsf.handler.URLMatcher.URL_MATCHER_ATTACHMENT_KEY;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.pac4j.undertow.account.Pac4jAccount;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import net.nnwsf.controller.annotation.AuthenticatedUser;
import net.nnwsf.controller.annotation.PathVariable;
import net.nnwsf.controller.annotation.RequestBody;
import net.nnwsf.controller.annotation.RequestParameter;
import net.nnwsf.controller.converter.ContentTypeConverter;
import net.nnwsf.controller.converter.TextContentTypeConverter;
import net.nnwsf.controller.converter.annotation.Converter;
import net.nnwsf.util.ReflectionHelper;
import net.nnwsf.util.TypeUtil;
import net.nnwsf.authentication.annotation.User;

public class EndpointHandlerImpl implements HttpHandler {

	private final static Logger log = Logger.getLogger(EndpointHandlerImpl.class.getName());

	private final Map<String, ContentTypeConverter> contentTypeConverters;
	private final ContentTypeConverter defaultContentTypeConverter = new TextContentTypeConverter();

	public EndpointHandlerImpl(Collection<Class<ContentTypeConverter>> converterClasses) {
		this.contentTypeConverters = converterClasses.stream()
			.map(converterClass -> {
				ContentTypeConverter converter = ReflectionHelper.getInstance(converterClass);
				Converter annotation = ReflectionHelper.findAnnotation(converterClass, Converter.class);
				return Map.entry(annotation.contentType(), converter);
			})
			.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue(), (first, second) -> first));
	}

	@Override
	public void handleRequest(final HttpServerExchange exchange) throws Exception {
		log.log(Level.FINEST, "Endpoint request: start");
		EndpointProxy endpointProxy = exchange.getAttachment(ENDPOINT_PROXY_ATTACHMENT_KEY);
		URLMatcher requestUrlMatcher = exchange.getAttachment(URL_MATCHER_ATTACHMENT_KEY);

		Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

		if(endpointProxy != null) {
			Object[] parameters = new Object[endpointProxy.getParametersCount()];
			for(int i = 0; i<endpointProxy.getParameters().length; i++) {
				AnnotatedMethodParameter annotatedMethodParameter = endpointProxy.getParameters()[i];
				if (annotatedMethodParameter != null) {
					if (annotatedMethodParameter.getAnnotation().annotationType().isAssignableFrom(RequestParameter.class)) {
						Deque<String> parameterValue = queryParameters.get(annotatedMethodParameter.getName());
						if (parameterValue != null) {
							String value = parameterValue.element();
							parameters[i] = TypeUtil.toType(value, annotatedMethodParameter.getType());
						}
					} else if (annotatedMethodParameter.getAnnotation().annotationType().isAssignableFrom(PathVariable.class)) {
						int index = endpointProxy.getPathElements().indexOf("{" + annotatedMethodParameter.getName() + "}");
						String value = requestUrlMatcher.getPathElements()[index];
						parameters[i] = TypeUtil.toType(value, annotatedMethodParameter.getType());
					}
				}
			}
			ContentTypeConverter contentTypeConverter = getContentTypeConverter(endpointProxy.getContentType());

			if(endpointProxy.getSpecialRequestParameter(RequestBody.class) != null) {
				MethodParameter parameter = endpointProxy.getSpecialRequestParameter(RequestBody.class);
				exchange.startBlocking();
				parameters[parameter.getIndex()] = contentTypeConverter.readFrom(exchange.getInputStream(), parameter.getType());
			}
			if(endpointProxy.getSpecialRequestParameter(AuthenticatedUser.class) != null) {
				MethodParameter parameter = endpointProxy.getSpecialRequestParameter(AuthenticatedUser.class);
				if(exchange.getSecurityContext() != null && exchange.getSecurityContext().getAuthenticatedAccount() != null) {
					parameters[parameter.getIndex()] = new User((Pac4jAccount)exchange.getSecurityContext().getAuthenticatedAccount());
				}
			}
			if(endpointProxy.getSpecialRequestParameter(HttpServerExchange.class) != null) {
				MethodParameter parameter = endpointProxy.getSpecialRequestParameter(RequestBody.class);
				parameters[parameter.getIndex()] = exchange;
			}
			try {
				Object result = endpointProxy.invoke(parameters);
				if(!exchange.isComplete()) {
					if(result == null) {
						exchange.setStatusCode(200).getResponseSender().send("");
					} else {
						exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, endpointProxy.getContentType());
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
			log.log(Level.SEVERE, "Unable to find endpoint for " + exchange.getRequestPath() + exchange.getQueryString());
			exchange.setStatusCode(404);
		}
		log.log(Level.FINEST, "Endpoint request: end");
	}

	private ContentTypeConverter getContentTypeConverter(String contentType) {
		ContentTypeConverter converter = contentTypeConverters.get(contentType);
		if(converter == null) {
			converter = defaultContentTypeConverter;
		}
		return converter;
	}
}