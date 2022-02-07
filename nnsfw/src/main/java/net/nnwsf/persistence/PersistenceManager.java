package net.nnwsf.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
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
import javax.persistence.Persistence;
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
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;

import net.nnwsf.application.annotation.DatasourceConfiguration;
import net.nnwsf.application.annotation.FlywayConfiguration;
import net.nnwsf.configuration.ConfigurationManager;
import net.nnwsf.nocode.NocodeManager;
import net.nnwsf.persistence.annotation.Repository;
import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.ProxyUtil;

public class PersistenceManager {
	public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
 
    public static final String JPA_VERSION = "2.1";
 
    private final String persistenceUnitName;
 
    private PersistenceUnitTransactionType transactionType =
        PersistenceUnitTransactionType.RESOURCE_LOCAL;
 
    private final Collection<Class<?>> managedClasses;
 
    private final List<String> mappingFileNames = new ArrayList<>();
 
    private final Properties properties;
 
    private DataSource jtaDataSource;
 
    private DataSource nonJtaDataSource;
 
    public PersistenceUnitInfoImpl(
            String persistenceUnitName,
            Collection<Class<?>> managedClasses,
            Properties properties) {
        this.persistenceUnitName = persistenceUnitName;
        this.managedClasses = managedClasses;
        this.properties = properties;
    }
 
    @Override
    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }
 
    @Override
    public String getPersistenceProviderClassName() {
        return HibernatePersistenceProvider.class.getName();
    }
 
    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return transactionType;
    }
 
    @Override
    public DataSource getJtaDataSource() {
        return jtaDataSource;
    }
 
    public PersistenceUnitInfoImpl setJtaDataSource(
            DataSource jtaDataSource) {
        this.jtaDataSource = jtaDataSource;
        this.nonJtaDataSource = null;
        transactionType = PersistenceUnitTransactionType.JTA;
        return this;
    }
 
    @Override
    public DataSource getNonJtaDataSource() {
        return nonJtaDataSource;
    }
 
    public PersistenceUnitInfoImpl setNonJtaDataSource(
            DataSource nonJtaDataSource) {
        this.nonJtaDataSource = nonJtaDataSource;
        this.jtaDataSource = null;
        transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
        return this;
    }
 
    @Override
    public List<String> getMappingFileNames() {
        return mappingFileNames;
    }
 
    @Override
    public List<URL> getJarFileUrls() {
        return Collections.emptyList();
    }
 
    @Override
    public URL getPersistenceUnitRootUrl() {
        return null;
    }
 
    @Override
    public List<String> getManagedClassNames() {
        return managedClasses.stream().map(Class::getName).collect(Collectors.toList());
    }
 
    @Override
    public boolean excludeUnlistedClasses() {
        return false;
    }
 
    @Override
    public SharedCacheMode getSharedCacheMode() {
        return SharedCacheMode.UNSPECIFIED;
    }
 
    @Override
    public ValidationMode getValidationMode() {
        return ValidationMode.AUTO;
    }
 
    public Properties getProperties() {
        return properties;
    }
 
    @Override
    public String getPersistenceXMLSchemaVersion() {
        return JPA_VERSION;
    }
 
    @Override
    public ClassLoader getClassLoader() {
        return NocodeManager.getClassLoader();
    }
 
    @Override
    public void addTransformer(ClassTransformer transformer) {
 
    }
 
    @Override
    public ClassLoader getNewTempClassLoader() {
        return null;
    }
}

	private static Logger log = Logger.getLogger(PersistenceManager.class.getName());

    private static PersistenceManager instance;

    public static PersistenceManager init() {

        if(instance == null) {
			try {
				Map<Repository, Class<PersistenceRepository>> repositoryClasses = ClassDiscovery.discoverAnnotatedClasses(PersistenceRepository.class, Repository.class);
				Map<FlywayConfiguration, Class<Object>> flywayConfigurationClasses = ClassDiscovery.discoverAnnotatedClasses(Object.class, FlywayConfiguration.class);
				Map<Entity, Class<Object>> entityClassAnnotations = ClassDiscovery.discoverAnnotatedClasses(Object.class, Entity.class);
				Collection<Class<?>> entityClasses = new ArrayList<>(entityClassAnnotations.values());
				entityClasses.addAll(NocodeManager.getEntityClasses());
				instance = new PersistenceManager( repositoryClasses, entityClasses, flywayConfigurationClasses);
			} catch (Exception e) {
                log.log(Level.SEVERE, "Unable to discover repositories or datasources", e);
                throw new RuntimeException("Unable to discover repositories or datasources", e);
            }

        }
        return instance;
    }

	private final Map<Class<PersistenceRepository>, Repository> repositoryClassesMap;
	private final Collection<Class<?>> entityClasses;
    private final Map<String, EntityManagerFactory> entityManagerFactoryMap;
    private final Map<String, ThreadLocal<EntityManager>> entityManagerThreadLocalMap;

	private PersistenceManager(
		Map<Repository, Class<PersistenceRepository>> repositoryClasses,
		Collection<Class<?>> entityClasses,
		Map<FlywayConfiguration, Class<Object>> flywayConfigurationClasses) {
		this.entityManagerThreadLocalMap = new HashMap<>();
		this.repositoryClassesMap = new HashMap<>();
		this.entityClasses = entityClasses;
		this.entityManagerFactoryMap = new HashMap<>();
		DatasourceManager.getDatasourceConfigurations().forEach(aDatasource ->
			initEntityManager(aDatasource)
		);
		for(Entry<FlywayConfiguration, Class<Object>> entry : flywayConfigurationClasses.entrySet()) {
			FlywayConfiguration hydratedFlywayConfiguration = ConfigurationManager.apply(entry.getKey());
			initFlyway(hydratedFlywayConfiguration);
		}
		for(Entry<Repository, Class<PersistenceRepository>> entry : repositoryClasses.entrySet()) {
			this.repositoryClassesMap.put(entry.getValue(), entry.getKey());
		}
		this.repositoryClassesMap.putAll(NocodeManager.getPersistenceClasses());
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


            BootstrapServiceRegistryBuilder bootstrapRegistryBuilder =
            new BootstrapServiceRegistryBuilder();
            // add a custom ClassLoader
            bootstrapRegistryBuilder.applyClassLoader( NocodeManager.getClassLoader() );
            
            BootstrapServiceRegistry bootstrapRegistry = bootstrapRegistryBuilder.build();

            StandardServiceRegistry standardRegistry = StandardServiceRegistryBuilder
                .forJpa(bootstrapRegistry)
                .applySetting("connection.driver_class", datasourceConfiguration.jdbcDriver())
                .applySetting("hibernate.connection.url", datasourceConfiguration.jdbcUrl())
                .applySetting("hibernate.connection.username", datasourceConfiguration.user() == null ? "" : datasourceConfiguration.user())
                .applySetting("hibernate.connection.password", datasourceConfiguration.password() == null ? "" : datasourceConfiguration.password())
                .applySetting("show_sql", "true")
				.applySettings(properties)
                .build();

            MetadataSources sources = new MetadataSources(standardRegistry);
            entityClasses.forEach(aClass -> sources.addAnnotatedClass(aClass));

            MetadataBuilder metadataBuilder = sources.getMetadataBuilder();

            Metadata metadata = metadataBuilder.build();

			EntityManagerFactory entityManagerFactory = metadata.getSessionFactoryBuilder().build();
			entityManagerFactoryMap.put(datasourceConfiguration.name(), entityManagerFactory);
		} catch(Exception e) {
			log.log(Level.SEVERE, "Unable to initialize persistence", e);
			throw new RuntimeException(e);
		}
		this.entityManagerThreadLocalMap.put(datasourceConfiguration.name(),new ThreadLocal<>());

	}
	
	private void initFlyway(FlywayConfiguration flywayConfiguration) {
		DatasourceConfiguration datasourceConfiguration = DatasourceManager.getDatasourceConfiguration(flywayConfiguration.datasource());
		if(datasourceConfiguration == null) {
			log.log(Level.SEVERE, "Unable to find datasource {} for FlywayConfiguration", flywayConfiguration.datasource());
			throw new RuntimeException("Unable to find datasource for FlywayConfiguration");
		}
		if(flywayConfiguration != null) {
			FluentConfiguration flywayFluentConfiguration = Flyway.configure()
				.locations(flywayConfiguration.location())
                .baselineOnMigrate(true)
                .baselineVersion("0")
				.dataSource(
					datasourceConfiguration.jdbcUrl(),
					datasourceConfiguration.user(), 
					datasourceConfiguration.password());
			if(!"".equals(datasourceConfiguration.schema())) {
				flywayFluentConfiguration.schemas(datasourceConfiguration.schema());
			}
			Flyway load = flywayFluentConfiguration.load();
            load.migrate();
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

