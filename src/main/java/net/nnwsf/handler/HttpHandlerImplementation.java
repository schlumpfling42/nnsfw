package net.nnwsf.handler;

import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import net.nnwsf.controller.Controller;
import net.nnwsf.controller.Get;
import net.nnwsf.controller.PathVariable;
import net.nnwsf.controller.Post;
import net.nnwsf.controller.RequestBody;
import net.nnwsf.controller.RequestParameter;
import net.nnwsf.util.Reflection;

public class HttpHandlerImplementation implements HttpHandler {

	private final static Logger log = Logger.getLogger(HttpHandlerImplementation.class.getName());

	private class URLMatcher {
		private final String httpMethod;
		private String[] pathElements;
		private Collection<String> queryParameterNames;

		URLMatcher(String httpMethod, String path, Collection<String> queryParameterNames) {
			this.httpMethod = httpMethod.toUpperCase();
			Collection<String> pathElementCollection = new ArrayList<>();
			StringTokenizer pathTokenizer = new StringTokenizer(path, "/");
			while(pathTokenizer.hasMoreTokens()) {
				String nextToken = pathTokenizer.nextToken();
				if(nextToken != null && !"".equals(nextToken)) {
					pathElementCollection.add(nextToken);
				}
			}

			this.pathElements = pathElementCollection.toArray(new String[pathElementCollection.size()]);
			this.queryParameterNames = new HashSet<>(queryParameterNames);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getEnclosingInstance().hashCode();
			result = prime * result + httpMethod.hashCode();
			result = prime * result + pathElements.length;
			result = prime * result + ((queryParameterNames == null) ? 0 : queryParameterNames.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			URLMatcher other = (URLMatcher) obj;
			if (!getEnclosingInstance().equals(other.getEnclosingInstance())) {
				return false;
			}
			if (!Objects.equals(httpMethod, other.httpMethod)) {
				return false;
			}
			if (pathElements.length != other.pathElements.length) {
				return false;
			}
			for(int i=0; i<pathElements.length; i++) {
				if(!pathElements[i].startsWith("{") && !pathElements[i].endsWith("{") && !other.pathElements[i].startsWith("{") && !other.pathElements[i].endsWith("{") && !Objects.equals(pathElements[i], other.pathElements[i])) {
					return false;
				}
			}
			if (queryParameterNames == null) {
				if (other.queryParameterNames != null)
					return false;
			} else if (!queryParameterNames.equals(other.queryParameterNames)) {
				return false;
			}
			return true;
		}

		private HttpHandlerImplementation getEnclosingInstance() {
			return HttpHandlerImplementation.this;
		}

		@Override
		public String toString() {
			return "URLMatcher [httpMethod=" + httpMethod + ", pathElements=" + Arrays.toString(pathElements)
					+ ", queryParameterNames=" + queryParameterNames + "]";
		}

		
	}

	private class MethodParameter {
		private final Annotation annotation;
		private final String name;
		private final Class<?> type;
		MethodParameter(Annotation annotation, String name, Class<?> type) {
			this.annotation = annotation;
			this.name = name;
			this.type = type;
		}

		@Override
		public String toString() {
			return "MethodParameter [annotation=" + annotation + ", name=" + name + ", type=" + type + "]";
		}

	}

	private class ControllerProxy {
		private final Method method;
		private final Object instance;
		private final MethodParameter[] methodParameters;
		private final List<String> pathElements;

		ControllerProxy(Object instance, Method method, MethodParameter[] methodParameters, List<String> pathElements) {
			this.instance = instance;
			this.method = method;
			this.methodParameters = methodParameters;
			this.pathElements = pathElements;
		}

		@Override
		public String toString() {
			return "ControllerProxy [class=" + instance.getClass() + ", method=" + method + ", parameterNames="
					+ Arrays.toString(methodParameters) + "]";
		}
	}

	private final Map<URLMatcher, ControllerProxy> proxies = new HashMap<>();
	private final Collection<Class<?>> controllerClasses;
	private final Gson gson;


	public HttpHandlerImplementation(Collection<Class<?>> controllerClasses) {
		this.controllerClasses = controllerClasses;
		this.gson = new GsonBuilder().create();
	}

	@Override
	public void handleRequest(final HttpServerExchange exchange) throws Exception {
		if (exchange.isInIoThread()) {
     		exchange.dispatch(this);
      		return;
    	}
		log.log(Level.INFO, "HttpRequest: start");
		HttpString method = exchange.getRequestMethod();
		Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();

		URLMatcher requestUrlMatcher = new URLMatcher(method.toString(), exchange.getRequestPath(), queryParameters.keySet());

		ControllerProxy proxy = findController(requestUrlMatcher);
		if(proxy != null) {
			Object[] parameters = new Object[proxy.method.getParameterTypes().length];
			for(int i=0; i<proxy.methodParameters.length; i++) {
				MethodParameter methodParameter = proxy.methodParameters[i];
				if(methodParameter != null) {
					if(methodParameter.annotation.annotationType().isAssignableFrom(RequestParameter.class)) {
						Deque<String> parameterValue = queryParameters.get(methodParameter.name);
						if(parameterValue != null) {
							parameters[i] = parameterValue.element();
						}
					} else if(methodParameter.annotation.annotationType().isAssignableFrom(PathVariable.class)) {
						int index = proxy.pathElements.indexOf("{"+methodParameter.name+"}");
						parameters[i] = requestUrlMatcher.pathElements[index];
					} else if(methodParameter.annotation.annotationType().isAssignableFrom(RequestBody.class)) {
						StringBuilder body = new StringBuilder();
						exchange.startBlocking();
						try (InputStreamReader reader = new InputStreamReader(exchange.getInputStream(), Charset.defaultCharset())) {
							char[] buffer = new char[256];
							int read;
							while ((read = reader.read(buffer)) != -1) {
								body.append(buffer, 0, read);
							}
						}
						if(exchange.getRequestHeaders().get(Headers.CONTENT_TYPE).contains("application/json")) {
							parameters[i] = gson.fromJson(body.toString(), methodParameter.type);
						} else if(methodParameter.type.isAssignableFrom(String.class)) {
							parameters[i] = body.toString();
						}
					}
				}
			}
			Object result = proxy.method.invoke(proxy.instance, parameters);
			exchange.setStatusCode(200).getResponseSender().send(result.toString());
		} else {
			log.log(Level.SEVERE, "Unable to find controller for " + exchange.getRequestPath() + exchange.getQueryString());
			exchange.setStatusCode(404);
		}
		log.log(Level.INFO, "HttpRequest: end");
	}

	private ControllerProxy findController(URLMatcher requestUrlMatcher) throws Exception {
		ControllerProxy proxy = proxies.get(requestUrlMatcher);
		if(proxy != null) {
			return proxy;
		}

		for(Class<?> aClass : controllerClasses) {
			Controller controllerAnnotation = Reflection.getInstance().findAnnotation(aClass, Controller.class);
			Map<Get, Collection<Method>> annotatedGetMethods = Reflection.getInstance().findAnnotationMethods(aClass, Get.class);
			Map<Post, Collection<Method>> annotatedPostMethods = Reflection.getInstance().findAnnotationMethods(aClass, Post.class);
			Object object = aClass.getDeclaredConstructor().newInstance();
			if(annotatedGetMethods == null) {
				throw new RuntimeException("Invalid controller");
			}
			for(Get methodAnnotation : annotatedGetMethods.keySet()) {
				for(Method annotatedMethod : annotatedGetMethods.get(methodAnnotation)) {
					if(annotatedMethod == null) {
						throw new RuntimeException("Invalid controller");
					}
					MethodParameter[] methodParameters = getMethodParameters(annotatedMethod);
					URLMatcher proxyUrlMatcher = new URLMatcher(
						"Get", 
						(controllerAnnotation.value() + "/" + methodAnnotation.value()).replace("/+", "/"), 
						Arrays.asList(methodParameters)
							.stream()
							.filter( s -> s != null && s.annotation.annotationType().isAssignableFrom(RequestParameter.class))
							.map(m -> m.name)
							.filter( s -> s != null).collect(Collectors.toList()));
					if(proxies.containsKey(proxyUrlMatcher)) {
						log.log(Level.SEVERE, "Controller already exists for " + proxyUrlMatcher);
					}
					proxies.put(proxyUrlMatcher, new ControllerProxy(object, annotatedMethod, methodParameters, Arrays.asList(proxyUrlMatcher.pathElements)));
				}
			}

			for(Post methodAnnotation : annotatedPostMethods.keySet()) {
				for(Method annotatedMethod : annotatedPostMethods.get(methodAnnotation)) {
					if(annotatedMethod == null) {
						throw new RuntimeException("Invalid controller");
					}
					MethodParameter[] methodParameters = getMethodParameters(annotatedMethod);
					URLMatcher proxyUrlMatcher = new URLMatcher(
						"Post", 
						(controllerAnnotation.value() + "/" + methodAnnotation.value()).replace("/+", "/"), 
						Arrays.asList(methodParameters)
							.stream()
							.filter( s -> s != null && s.annotation.annotationType().isAssignableFrom(RequestParameter.class))
							.map(m -> m.name)
							.filter( s -> s != null).collect(Collectors.toList()));
					if(proxies.containsKey(proxyUrlMatcher)) {
						log.log(Level.SEVERE, "Controller already exists for " + proxyUrlMatcher);
					}
					proxies.put(proxyUrlMatcher, new ControllerProxy(object, annotatedMethod, methodParameters, Arrays.asList(proxyUrlMatcher.pathElements)));
				}
			}

			controllerClasses.remove(aClass);

		}
		return proxies.get(requestUrlMatcher);
	}

	private MethodParameter[] getMethodParameters(Method annotatedMethod) {
		Annotation[][] parameterAnnotations = annotatedMethod.getParameterAnnotations();
		MethodParameter[] methodParameters = new MethodParameter[parameterAnnotations.length];
		for(int i = 0; i< parameterAnnotations.length; i++) {
			if(parameterAnnotations[i] != null ) {
				for(Annotation aParameterAnnotation : parameterAnnotations[i]) {
					if(aParameterAnnotation.annotationType().isAssignableFrom(RequestParameter.class)) {
						methodParameters[i] = new MethodParameter(aParameterAnnotation, ((RequestParameter)aParameterAnnotation).value(), annotatedMethod.getParameterTypes()[i]);
					} else if(aParameterAnnotation.annotationType().isAssignableFrom(PathVariable.class)) {
						methodParameters[i] = new MethodParameter(aParameterAnnotation, ((PathVariable)aParameterAnnotation).value(), annotatedMethod.getParameterTypes()[i]);
					} else if(aParameterAnnotation.annotationType().isAssignableFrom(RequestBody.class)) {
						methodParameters[i] = new MethodParameter(aParameterAnnotation, "body", annotatedMethod.getParameterTypes()[i]);
					}
				}
			}
		}
		return methodParameters;
	}
}