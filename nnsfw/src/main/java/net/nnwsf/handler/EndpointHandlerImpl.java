package net.nnwsf.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.pac4j.core.profile.UserProfile;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.RoutingContext;
import net.nnwsf.authentication.User;
import net.nnwsf.controller.annotation.AuthenticatedUser;
import net.nnwsf.controller.annotation.PathVariable;
import net.nnwsf.controller.annotation.RequestBody;
import net.nnwsf.controller.annotation.RequestParameter;
import net.nnwsf.controller.converter.ContentTypeConverter;
import net.nnwsf.controller.converter.TextContentTypeConverter;
import net.nnwsf.controller.converter.annotation.Converter;
import net.nnwsf.util.ReflectionHelper;
import net.nnwsf.util.TypeUtil;

public class EndpointHandlerImpl {

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

	public Uni<Void> handleRequest(final RoutingContext routingContext, EndpointProxy endpointProxy, UserProfile userProfile) throws Exception {
		log.log(Level.FINEST, "Endpoint request: start");

		MultiMap queryParameters = routingContext.queryParams();

		if(endpointProxy != null) {
			Object[] parameters = new Object[endpointProxy.getParametersCount()];
			for(int i = 0; i<endpointProxy.getParameters().length; i++) {
				AnnotatedMethodParameter annotatedMethodParameter = endpointProxy.getParameters()[i];
				if (annotatedMethodParameter != null) {
					if (annotatedMethodParameter.getAnnotation().annotationType().isAssignableFrom(RequestParameter.class)) {
						String parameterValue = queryParameters.get(annotatedMethodParameter.getName());
						if (parameterValue != null) {
							parameters[i] = TypeUtil.toType(parameterValue, annotatedMethodParameter.getType());
						}
					} else if (annotatedMethodParameter.getAnnotation().annotationType().isAssignableFrom(PathVariable.class)) {
						String value = routingContext.pathParam(annotatedMethodParameter.getName());
						parameters[i] = TypeUtil.toType(value, annotatedMethodParameter.getType());
					}
				}
			}
			ContentTypeConverter contentTypeConverter = getContentTypeConverter(endpointProxy.getContentType());

			if(endpointProxy.getSpecialRequestParameter(RequestBody.class) != null) {
				MethodParameter parameter = endpointProxy.getSpecialRequestParameter(RequestBody.class);
				parameters[parameter.getIndex()] = contentTypeConverter.readFrom(new ByteArrayInputStream(routingContext.getBody().getBytes()), parameter.getType());
			}
			if(endpointProxy.getSpecialRequestParameter(AuthenticatedUser.class) != null) {
				MethodParameter parameter = endpointProxy.getSpecialRequestParameter(AuthenticatedUser.class);
				if(routingContext.session() != null) {
					parameters[parameter.getIndex()] = new User(userProfile);
				}
			}
			try {
				return endpointProxy.invoke(parameters)
					.withContext((resultUni, context) -> 
						resultUni.chain(result -> {
							routingContext.response().putHeader("Content-Type", endpointProxy.getContentType());
							if( result == null) {
								return routingContext.response().end();
							}
							Buffer outputBuffer = Buffer.buffer();
							try {
								contentTypeConverter.writeTo(result, outputBuffer);
							} catch(IOException ioe) {
								return Uni.createFrom().failure(ioe);
							}
							routingContext.response().setChunked(true);
							return routingContext.response().end(outputBuffer);
						}));
			} catch(InvocationTargetException ite) {
				if(ite.getCause() != null) {
					throw (Exception)ite.getCause();
				}
				throw ite;
			}
		} else {
			//log.log(Level.SEVERE, "Unable to find endpoint for " + exchange.getRequestPath() + exchange.getQueryString());
			routingContext.fail(404);
			return routingContext.end();
		}
	}

	private ContentTypeConverter getContentTypeConverter(String contentType) {
		ContentTypeConverter converter = contentTypeConverters.get(contentType);
		if(converter == null) {
			converter = defaultContentTypeConverter;
		}
		return converter;
	}
}