package net.nnwsf.persistence;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.nnwsf.application.annotation.DatasourceConfiguration;
import net.nnwsf.application.annotation.DatasourceConfigurations;
import net.nnwsf.configuration.ConfigurationManager;
import net.nnwsf.util.ClassDiscovery;

public class DatasourceManager {

    private static DatasourceManager instance;

    public static void init() {
        try {
        Map<DatasourceConfiguration, Class<Object>> datasourceClasses = ClassDiscovery.discoverAnnotatedClasses(Object.class, DatasourceConfiguration.class);
        Map<DatasourceConfigurations, Class<Object>> datasourcesClasses = ClassDiscovery.discoverAnnotatedClasses(Object.class, DatasourceConfigurations.class);
        if(datasourcesClasses != null) {
            datasourcesClasses.entrySet().forEach(anEntry -> 
                Arrays.stream(anEntry.getKey().value()).forEach(aDatasource -> datasourceClasses.put(aDatasource, anEntry.getValue()))
            );
        }
        instance = new DatasourceManager(datasourceClasses);
        } catch(Exception e) {
            throw new RuntimeException("Unable to find datasources", e);
        }
    }

    public static Collection<DatasourceConfiguration> getDatasourceConfigurations() {
        return instance.datasourceConfigurationMap.values();
    }

    public static DatasourceConfiguration getDatasourceConfiguration(String name) {
        return instance.datasourceConfigurationMap.get(name);
    }

	private final Map<String, DatasourceConfiguration> datasourceConfigurationMap;

    DatasourceManager(Map<DatasourceConfiguration, Class<Object>> datasourceClasses) {
		this.datasourceConfigurationMap = new HashMap<>();
		for(DatasourceConfiguration datasource : datasourceClasses.keySet()) {
			DatasourceConfiguration hydratedDatasourceConfiguration = ConfigurationManager.apply(datasource);
			this.datasourceConfigurationMap.put(hydratedDatasourceConfiguration.name(), hydratedDatasourceConfiguration);
		}
    }
}
