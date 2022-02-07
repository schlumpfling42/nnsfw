package net.nnwsf.handler.nocode;

import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

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
			proxies.add(new ControllerProxyNocodeFindAllImplementation(wellFormedControllerRootPath, "GET", aSchemaObject));
			proxies.add(new ControllerProxyNocodeFindImplementation(wellFormedControllerRootPath, "GET", aSchemaObject));
			proxies.add(new ControllerProxyNocodeCreateImplementation(wellFormedControllerRootPath, "PUT", aSchemaObject));
			proxies.add(new ControllerProxyNocodeSaveImplementation(wellFormedControllerRootPath, "POST", aSchemaObject));
			proxies.add(new ControllerProxyNocodeDeleteImplementation(wellFormedControllerRootPath, "DELETE", aSchemaObject));
		});
}

	public Collection<EndpointProxy> getProxies() {
		return proxies;
	}
}