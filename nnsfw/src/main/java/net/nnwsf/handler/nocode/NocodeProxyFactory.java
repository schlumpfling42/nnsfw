package net.nnwsf.handler.nocode;

import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.nnwsf.controller.documentation.annotation.ApiDoc;
import net.nnwsf.handler.EndpointProxy;
import net.nnwsf.nocode.NocodeManager;

public class NocodeProxyFactory {

	private final ObjectMapper objectMapper;
	private final Collection<EndpointProxy> proxies;

	public NocodeProxyFactory() {
		String controllerRootPath = NocodeManager.getControllerPath();
		String wellFormedControllerRootPath = ("/" + ((controllerRootPath == null || controllerRootPath.isBlank()) ? "" : controllerRootPath)).replaceAll("/+", "/");
		this.objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		proxies = new ArrayList<>();
		NocodeManager.getSchemas().forEach(aSchemaObject -> {
            Builder<?> builder = new ByteBuddy().subclass(NocodeController.class).name("Nocode" + aSchemaObject.getTitle() + "Controller");
            builder = builder.annotateType(AnnotationDescription.Builder.ofType(ApiDoc.class)
                .define("value", aSchemaObject.getDescription()).build());
			Class<?> controllerClass =  builder.make().load(NocodeController.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION).getLoaded();
			proxies.add(new ControllerProxyNocodeFindAllImplementation(wellFormedControllerRootPath, "GET", aSchemaObject, controllerClass));
			proxies.add(new ControllerProxyNocodeFindImplementation(wellFormedControllerRootPath, "GET", aSchemaObject, controllerClass));
			proxies.add(new ControllerProxyNocodeCreateImplementation(wellFormedControllerRootPath, "PUT", aSchemaObject, controllerClass));
			proxies.add(new ControllerProxyNocodeSaveImplementation(wellFormedControllerRootPath, "POST", aSchemaObject, controllerClass));
			proxies.add(new ControllerProxyNocodeDeleteImplementation(wellFormedControllerRootPath, "DELETE", aSchemaObject, controllerClass));
		});
}

	public Collection<EndpointProxy> getProxies() {
		return proxies;
	}
}