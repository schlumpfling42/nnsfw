package net.nnwsf.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.persistence.Entity;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.Mutiny.Session;
import org.hibernate.reactive.provider.ReactiveServiceRegistryBuilder;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import net.nnwsf.application.annotation.DatasourceConfiguration;
import net.nnwsf.application.annotation.FlywayConfiguration;
import net.nnwsf.configuration.ConfigurationManager;
import net.nnwsf.nocode.NocodeManager;
import net.nnwsf.persistence.annotation.Repository;
import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.ProxyUtil;

public class PersistenceManager {
	private static Logger log = Logger.getLogger(PersistenceManager.class.getName());

    private static PersistenceManager instance;

    @SuppressWarnings("rawtypes")
    public static PersistenceManager init(Vertx vertx) {

        if(instance == null) {
			try {
				Map<Repository, Class<PersistenceRepository>> repositoryClasses = ClassDiscovery.discoverAnnotatedClasses(PersistenceRepository.class, Repository.class);
				Map<FlywayConfiguration, Class<Object>> flywayConfigurationClasses = ClassDiscovery.discoverAnnotatedClasses(Object.class, FlywayConfiguration.class);
				Map<Entity, Class<Object>> entityClassAnnotations = ClassDiscovery.discoverAnnotatedClasses(Object.class, Entity.class);
				Collection<Class<?>> entityClasses = new ArrayList<>(entityClassAnnotations.values());
				entityClasses.addAll(NocodeManager.getEntityClasses());
				instance = new PersistenceManager( vertx, repositoryClasses, entityClasses, flywayConfigurationClasses);
			} catch (Exception e) {
                log.log(Level.SEVERE, "Unable to discover repositories or datasources", e);
                throw new RuntimeException("Unable to discover repositories or datasources", e);
            }

        }
        return instance;
    }

    @SuppressWarnings("rawtypes")
	private final Map<Class<PersistenceRepository>, Repository> repositoryClassesMap;
	private final Collection<Class<?>> entityClasses;
    private final Map<String, Mutiny.SessionFactory> sessionFactoryMap;

    @SuppressWarnings("rawtypes")
	private PersistenceManager(
		Vertx vertx,
		Map<Repository, Class<PersistenceRepository>> repositoryClasses,
		Collection<Class<?>> entityClasses,
		Map<FlywayConfiguration, Class<Object>> flywayConfigurationClasses) {
		this.repositoryClassesMap = new HashMap<>();
		this.entityClasses = entityClasses;
		this.sessionFactoryMap = new HashMap<>();
		DatasourceManager.getDatasourceConfigurations().forEach(aDatasource ->
			initEntityManager(vertx, aDatasource)
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
    private void initEntityManager(Vertx vertx, DatasourceConfiguration datasourceConfiguration) {
		try {
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
			
			Uni<Void> startHibernate = Uni.createFrom().deferred(() -> {
				BootstrapServiceRegistryBuilder bootstrapRegistryBuilder =
					new BootstrapServiceRegistryBuilder();
				// add a custom ClassLoader
				bootstrapRegistryBuilder.applyClassLoader( NocodeManager.getClassLoader() );
				
				
				BootstrapServiceRegistry bootstrapRegistry = bootstrapRegistryBuilder.build();
				
				StandardServiceRegistry standardRegistry = ReactiveServiceRegistryBuilder
					.forJpa(bootstrapRegistry)
					.applySetting("hibernate.connection.url", datasourceConfiguration.jdbcUrl())
					.applySetting("hibernate.connection.username",datasourceConfiguration.user() == null ? "" : datasourceConfiguration.user())
					.applySetting("hibernate.connection.password", datasourceConfiguration.password() == null ? "" : datasourceConfiguration.password())
					.applySetting("hibernate.connection.pool_size", datasourceConfiguration.maxConnections())
					.applySettings(properties)
					.build();

				MetadataSources sources = new MetadataSources(standardRegistry);
				entityClasses.forEach(aClass -> sources.addAnnotatedClass(aClass));
		
				MetadataBuilder metadataBuilder = sources.getMetadataBuilder();
		
				Metadata metadata = metadataBuilder.build();
		
				Mutiny.SessionFactory sessionFactory =  metadata.getSessionFactoryBuilder().build()
				.unwrap(Mutiny.SessionFactory.class);
				
				sessionFactoryMap.put(datasourceConfiguration.name(), sessionFactory);
				return Uni.createFrom().voidItem();
			  });
			  
			  vertx.executeBlockingAndAwait(startHibernate);
		} catch(Exception e) {
			log.log(Level.SEVERE, "Unable to initialize persistence", e);
			throw new RuntimeException(e);
		}
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

	public static Uni<Session> createSession(String datasourceName) {
        if(instance != null) {
            return instance.internalCreateSession(datasourceName);
        }
        return null;
    }

	private  Uni<Session> internalCreateSession(String datasourceName) {
        return sessionFactoryMap.get(datasourceName).openSession();
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

    @SuppressWarnings("rawtypes")
	public static String getDatasource(Class<PersistenceRepository> repositoryClass) {
		return instance.repositoryClassesMap.get(repositoryClass).datasource();
	}

}

