package net.nnwsf.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import com.google.common.collect.ImmutableMap;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import net.nnwsf.application.annotation.DatasourceConfiguration;
import net.nnwsf.application.annotation.FlywayConfiguration;
import net.nnwsf.configuration.ConfigurationManager;
import net.nnwsf.persistence.annotation.Repository;
import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.ProxyUtil;

public class PersistenceManager {

    private final class PersistenceUnitInfoImplementation implements PersistenceUnitInfo {
		private final Class<? extends PersistenceProvider> persistenceProviderClass;

		private PersistenceUnitInfoImplementation(Class<? extends PersistenceProvider> persistenceProviderClass) {
			this.persistenceProviderClass = persistenceProviderClass;
		}

		@Override
		public ValidationMode getValidationMode() {
		    return ValidationMode.AUTO;
		}

		@Override
		public PersistenceUnitTransactionType getTransactionType() {
		    return PersistenceUnitTransactionType.RESOURCE_LOCAL;
		}

		@Override
		public SharedCacheMode getSharedCacheMode() {
		    return SharedCacheMode.ALL;
		}

		@Override
		public Properties getProperties() {
		    return new Properties();
		}

		@Override
		public String getPersistenceXMLSchemaVersion() {
		    return null;
		}

		@Override
		public URL getPersistenceUnitRootUrl() {
		    return null;
		}

		@Override
		public String getPersistenceUnitName() {
		    return "default";
		}

		@Override
		public String getPersistenceProviderClassName() {
		    return persistenceProviderClass.getCanonicalName();
		}

		@Override
		public DataSource getNonJtaDataSource() {
		    return null;
		}

		@Override
		public ClassLoader getNewTempClassLoader() {
		    return null;
		}

		@Override
		public List<String> getMappingFileNames() {
		    return Collections.emptyList();
		}

		@Override
		public List<String> getManagedClassNames() {
		    return entityClasses.stream().map(entityClass ->
				entityClass.getName())
				.collect(Collectors.toList());
		}

		@Override
		public DataSource getJtaDataSource() {
		    return null;
		}

		@Override
		public List<URL> getJarFileUrls() {
		    try {
		        return Collections.list(this.getClass()
		                                    .getClassLoader()
		                                    .getResources("*"));
		    } catch (IOException e) {
		        throw new UncheckedIOException(e);
		    }
		}

		@Override
		public ClassLoader getClassLoader() {
			return getClass().getClassLoader();
		}

		@Override
		public boolean excludeUnlistedClasses() {
		    return false;
		}

		@Override
		public void addTransformer(ClassTransformer transformer) {
		}
    }

	private static Logger log = Logger.getLogger(PersistenceManager.class.getName());

    private static PersistenceManager instance;

    public static PersistenceManager init() {

        if(instance == null) {
			try {
				Map<Repository, Class<PersistenceRepository>> repositoryClasses = ClassDiscovery.discoverAnnotatedClasses(PersistenceRepository.class, Repository.class);
				Map<DatasourceConfiguration, Class<Object>> datasourceClasses = ClassDiscovery.discoverAnnotatedClasses(Object.class, DatasourceConfiguration.class);
				Map<FlywayConfiguration, Class<Object>> flywayConfigurationClasses = ClassDiscovery.discoverAnnotatedClasses(Object.class, FlywayConfiguration.class);
				Map<Entity, Class<Object>> entityClassAnnotations = ClassDiscovery.discoverAnnotatedClasses(Object.class, Entity.class);
				Collection<Class<Object>> entityClasses = entityClassAnnotations.values();
				instance = new PersistenceManager(datasourceClasses, repositoryClasses, entityClasses, flywayConfigurationClasses);
			} catch (Exception e) {
                log.log(Level.SEVERE, "Unable to discover repositories or datasources", e);
                throw new RuntimeException("Unable to discover repositories or datasources", e);
            }

        }
        return instance;
    }

	private final Map<Class<?>, Repository> repositoryClassesMap;
	private final Collection<Class<Object>> entityClasses;
    private final Map<String, EntityManagerFactory> entityManagerFactoryMap;
    private final Map<String, ThreadLocal<EntityManager>> entityManagerThreadLocalMap;
	private final Map<String, DatasourceConfiguration> datasourceConfigurationMap;

	private PersistenceManager(
		Map<DatasourceConfiguration, Class<Object>> datasources, 
		Map<Repository, Class<PersistenceRepository>> repositoryClasses,
		Collection<Class<Object>> entityClasses,
		Map<FlywayConfiguration, Class<Object>> flywayConfigurationClasses) {
		this.entityManagerThreadLocalMap = new HashMap<>();
		this.repositoryClassesMap = new HashMap<>();
		this.entityClasses = entityClasses;
		this.entityManagerFactoryMap = new HashMap<>();
		this.datasourceConfigurationMap = new HashMap<>();
		for(DatasourceConfiguration datasource : datasources.keySet()) {
			DatasourceConfiguration hydratedDatasourceConfiguration = ConfigurationManager.apply(datasource);
			this.datasourceConfigurationMap.put(hydratedDatasourceConfiguration.name(), hydratedDatasourceConfiguration);
			initEntityManager(hydratedDatasourceConfiguration);
		}
		for(Entry<FlywayConfiguration, Class<Object>> entry : flywayConfigurationClasses.entrySet()) {
			FlywayConfiguration hydratedFlywayConfiguration = ConfigurationManager.apply(entry.getKey());
			initFlyway(hydratedFlywayConfiguration);
		}
		for(Entry<Repository, Class<PersistenceRepository>> entry : repositoryClasses.entrySet()) {
			this.repositoryClassesMap.put(entry.getValue(), entry.getKey());
		}
	}

	@SuppressWarnings("unchecked")
    private void initEntityManager(DatasourceConfiguration datasourceConfiguration) {
		try {
			if(datasourceConfiguration.providerClass() == null) {
				log.log(Level.SEVERE, "Unable to discover repositories, no persistenceProviderClass found");
				throw new RuntimeException("Unable to discover repositories, no persistenceProviderClass found");
			}
			if(datasourceConfiguration.jdbcDriver() == null || datasourceConfiguration.jdbcDriver().isEmpty()) {
				log.log(Level.SEVERE, "Unable to discover repositories, no jdbcDriver found");
				throw new RuntimeException("Unable to discover repositories, no jdbcDriver found");
			}
			if(datasourceConfiguration.jdbcUrl() == null || datasourceConfiguration.jdbcUrl().isEmpty()) {
				log.log(Level.SEVERE, "Unable to discover repositories, no jdbcUrl found");
				throw new RuntimeException("Unable to discover repositories, no jdbcUrl found");
			}
			Map<String, Object> properties = (Map<String, Object>)Optional.ofNullable(datasourceConfiguration.properties())
					.map(p -> 
						Arrays.stream(p)
						.collect(Collectors.toMap(v -> v.name(), v -> v.value()))
					).orElse(Collections.EMPTY_MAP);

			EntityManagerFactory entityManagerFactory = datasourceConfiguration.providerClass().getConstructor(new Class[0]).newInstance().createContainerEntityManagerFactory(
				new PersistenceUnitInfoImplementation(datasourceConfiguration.providerClass()),
				ImmutableMap.<String, Object>builder()
				.put("javax.persistence.jdbc.driver", datasourceConfiguration.jdbcDriver())
				.put("javax.persistence.jdbc.url", datasourceConfiguration.jdbcUrl())
				.put("javax.persistence.jdbc.user", datasourceConfiguration.user() == null ? "" : datasourceConfiguration.user())
				.put("javax.persistence.jdbc.password", datasourceConfiguration.password() == null ? "" : datasourceConfiguration.password())
				.putAll(properties)
				.build());
			entityManagerFactoryMap.put(datasourceConfiguration.name(), entityManagerFactory);
		} catch(Exception e) {
			log.log(Level.SEVERE, "Unable to initialize persistence", e);
			throw new RuntimeException(e);
		}
		this.entityManagerThreadLocalMap.put(datasourceConfiguration.name(),new ThreadLocal<>());

	}
	
	private void initFlyway(FlywayConfiguration flywayConfiguration) {
		DatasourceConfiguration datasourceConfiguration = this.datasourceConfigurationMap.get(flywayConfiguration.datasource());
		if(datasourceConfiguration == null) {
			log.log(Level.SEVERE, "Unable to find datasource {} for FlywayConfiguration", flywayConfiguration.datasource());
			throw new RuntimeException("Unable to find datasource for FlywayConfiguration");
		}
		if(flywayConfiguration != null) {
			FluentConfiguration flywayFluentConfiguration = Flyway.configure()
				.locations(flywayConfiguration.location())
				.dataSource(
					datasourceConfiguration.jdbcUrl(),
					datasourceConfiguration.user(), 
					datasourceConfiguration.password());
			if(!"".equals(datasourceConfiguration.schema())) {
				flywayFluentConfiguration.schemas(datasourceConfiguration.schema());
			}
			flywayFluentConfiguration.load().migrate();
		}
	}

	public static EntityManagerHolder createEntityManager(String datasourceName) {
        if(instance != null) {
            return instance.internalCreateEntityManager(datasourceName);
        }
        return null;
    }

	private EntityManagerHolder internalCreateEntityManager(String datasourceName) {
        boolean created = false;
        EntityManager entityManager = entityManagerThreadLocalMap.get(datasourceName).get();
        if(entityManager == null) {
            entityManager = entityManagerFactoryMap.get(datasourceName).createEntityManager();
            entityManagerThreadLocalMap.get(datasourceName).set(entityManager);
            if(entityManager != null) {
                created = true;
            }
        }

		return new EntityManagerHolder(entityManager, e -> entityManagerThreadLocalMap.get(datasourceName).remove(), created);
	}

	public static boolean isRepository(Class<?> aClass) {
        return instance.repositoryClassesMap.get(aClass) != null;
	}

	public static Object createRepository(Class<?> aClass) {
		return ProxyUtil.createProxy(
			aClass, 
			new RespositoryInterceptor(
				instance.repositoryClassesMap.get(aClass).entityClass(), 
				aClass,
				instance.repositoryClassesMap.get(aClass).datasource()
			)
		);
	}

}

