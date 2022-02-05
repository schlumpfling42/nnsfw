package net.nnwsf.handler.nocode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.nnwsf.util.ResourceUtil;
import net.nnwsf.handler.EndpointProxy;
import net.nnwsf.nocode.SchemaElement;

public class NocodeProxyFactory {

	private final Map<String, SchemaElement> parsedSchemas;
	private final ObjectMapper objectMapper;
	private final Collection<EndpointProxy> proxies;

	public NocodeProxyFactory(ClassLoader applicationClass, Collection<String> schemas, String controllerRootPath) {
		String wellFormedControllerRootPath = ("/" + ((controllerRootPath == null || controllerRootPath.isBlank()) ? "" : controllerRootPath)).replace("/+", "/");
		this.objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		this.parsedSchemas = schemas.stream()
			.map(aSchemaLocation -> {
				try {
				SchemaElement schemaElement = objectMapper.readValue(ResourceUtil.getResourceAsString(applicationClass, aSchemaLocation), SchemaElement.class);
				return Map.entry(schemaElement.getTitle(), schemaElement);
				} catch(Exception e) {
					throw new RuntimeException("Unable to load nocode schema: " + aSchemaLocation, e);
				}
			})
			.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue(), (first, second) -> first));

		proxies = new ArrayList<>();
		parsedSchemas.entrySet().forEach(anEntry -> {

				proxies.add(new ControllerProxyNocodeImplementation(wellFormedControllerRootPath, "GET", anEntry.getValue()));
			});
}

	public Collection<EndpointProxy> getProxies() {
		return proxies;
	}
}