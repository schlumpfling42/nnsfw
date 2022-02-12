package net.nnwsf.handler.controller;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.undertow.server.HttpServerExchange;
import net.nnwsf.util.InjectionHelper;
import net.nnwsf.util.ReflectionHelper;
import net.nnwsf.application.Constants;
import net.nnwsf.controller.annotation.AuthenticatedUser;
import net.nnwsf.controller.annotation.ContentType;
import net.nnwsf.controller.annotation.Controller;
import net.nnwsf.controller.annotation.Delete;
import net.nnwsf.controller.annotation.Get;
import net.nnwsf.controller.annotation.PathVariable;
import net.nnwsf.controller.annotation.Post;
import net.nnwsf.controller.annotation.Put;
import net.nnwsf.controller.annotation.RequestBody;
import net.nnwsf.controller.annotation.RequestParameter;
import net.nnwsf.handler.AnnotatedMethodParameter;
import net.nnwsf.handler.EndpointProxy;
import net.nnwsf.handler.MethodParameter;

public class ControllerProxyFactory {

	private final Collection<EndpointProxy> proxies;

	public ControllerProxyFactory(Collection<Class<Object>> controllerClasses) {
		proxies = new ArrayList<>();
		for (Class<?> aClass : controllerClasses) {
			Controller controllerAnnotation = ReflectionHelper.findAnnotation(aClass, Controller.class);
			Collection<Annotation> annotations = Arrays.asList(aClass.getAnnotations());
			Map<Annotation, Method> annotatedMethods = ReflectionHelper.findAnnotationMethods(aClass, Get.class,
					Post.class, Put.class, Delete.class);
			Object object;
			try {
				object = createObject(aClass);

				if (annotatedMethods == null) {
					throw new RuntimeException("Invalid controller");
				}
				for (Annotation methodAnnotation : annotatedMethods.keySet()) {
					Method annotatedMethod = annotatedMethods.get(methodAnnotation);
					ContentType contentTypeAnnotation = ReflectionHelper.findAnnotation(annotatedMethod, ContentType.class);
					String contentType = getContentType(contentTypeAnnotation);
					if (annotatedMethod == null) {
						throw new RuntimeException("Invalid controller");
					}
					AnnotatedMethodParameter[] annotatedMethodParameters = getMethodParameters(annotatedMethod);
					MethodParameter[] specialMethodParameters = getSpecialMethodParameters(annotatedMethod);
					proxies.add(new ControllerProxyMethodCallImplementation(object, methodAnnotation.annotationType().getSimpleName(), annotations, annotatedMethod, contentType,
									annotatedMethodParameters,
									specialMethodParameters, 
									(controllerAnnotation.value() + "/" + ReflectionHelper.getValue(methodAnnotation, "value")).replace("/+", "/")));
				}
			} catch (Exception e) {
				throw new RuntimeException("Unable to create controller: " + aClass.getName(), e);
			}
		}
	}
	
	public Collection<EndpointProxy> getProxies() {
		return proxies;
	}

	
	private Object createObject(Class<?> aClass) throws Exception {
		return InjectionHelper.getInjectable(aClass, null);
	}

	private String getContentType(ContentType annotation) {
		if (annotation != null && !"".equals(annotation.value())) {
			return annotation.value();
		}
		return Constants.CONTENT_TYPE_TEXT_HTML;
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
								annotatedMethod.getParameterTypes()[i],
								i);
					} else if (aParameterAnnotation.annotationType().isAssignableFrom(PathVariable.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation,
								((PathVariable) aParameterAnnotation).value(), annotatedMethod.getParameterTypes()[i], i);
					} else if (aParameterAnnotation.annotationType().isAssignableFrom(RequestBody.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, "body",
								annotatedMethod.getParameterTypes()[i], i);
					} else if (annotatedMethod.getParameters()[i].getType()
							.isAssignableFrom(HttpServletRequest.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, "request",
								annotatedMethod.getParameterTypes()[i], i);
					} else if (annotatedMethod.getParameters()[i].getType()
							.isAssignableFrom(HttpServletResponse.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, "response",
								annotatedMethod.getParameterTypes()[i], i);
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
								annotatedMethod.getParameterTypes()[i], i);
					} else if (aParameterAnnotation.annotationType().isAssignableFrom(AuthenticatedUser.class)) {
						annotatedMethodParameters[i] = new AnnotatedMethodParameter(aParameterAnnotation, "user",
								annotatedMethod.getParameterTypes()[i], i);
					}
				}
			}
		}
		Class<?>[] parameters = annotatedMethod.getParameterTypes();
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].isAssignableFrom(HttpServerExchange.class)) {
				annotatedMethodParameters[i] = new MethodParameter("exchange", parameters[i], i);
			}
		}
		return annotatedMethodParameters;
	}
}