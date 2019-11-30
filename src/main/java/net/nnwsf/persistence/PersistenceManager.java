package net.nnwsf.persistence;

import static org.hibernate.cfg.AvailableSettings.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import net.nnwsf.util.ClassDiscovery;
import net.nnwsf.util.Reflection;

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
		    return Collections.emptyList();
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
		                                    .getResources(""));
		    } catch (IOException e) {
		        throw new UncheckedIOException(e);
		    }
		}

		@Override
		public ClassLoader getClassLoader() {
		    return null;
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

    public static PersistenceManager init(
        Class<? extends PersistenceProvider> persistenceProviderClass,
        String jdbcDriver,
		String jdbcUrl,
		String jdbcDialect,
        String user,
        String password) {
        if(instance == null) {
			try {
                Map<Repository, Class<Object>> repositoryClasses = ClassDiscovery.discoverAnnotatedClasses(Object.class, Repository.class);
				instance = new PersistenceManager(
					repositoryClasses,
					persistenceProviderClass,
					jdbcDriver,
					jdbcUrl,
					jdbcDialect,
					user,
					password
				);
			} catch (Exception e) {
                log.log(Level.SEVERE, "Unable to discover repositories", e);
                throw new RuntimeException("Unable to discover repositories", e);
            }

        }
        return instance;
    }

	private final Map<Class<?>, Repository> repositoryClasses;
    private final EntityManagerFactory entityManagerFactory;
    private final ThreadLocal<EntityManager> entityManagerThreadLocal;

    public PersistenceManager(
		Map<Repository, Class<Object>> annotatedRepositoryClasses,
        Class<? extends PersistenceProvider> persistenceProviderClass,
        String jdbcDriver,
		String jdbcUrl,
		String jdbcDialect,
        String user,
        String password) {
			this.repositoryClasses = new HashMap<>();
			for(Map.Entry<Repository, Class<Object>> annotationClass : annotatedRepositoryClasses.entrySet()) {
				Class<?> aClass = annotationClass.getValue();
				this.repositoryClasses.put(aClass, annotationClass.getKey());
			}
            try {

            this.entityManagerFactory = persistenceProviderClass.newInstance().createContainerEntityManagerFactory(
                new PersistenceUnitInfoImplementation(persistenceProviderClass),
                ImmutableMap.<String, Object>builder()
                .put(JPA_JDBC_DRIVER, jdbcDriver)
                .put(JPA_JDBC_URL, jdbcUrl)
                .put(DIALECT, Class.forName(jdbcDialect))
                .put(USER, user)
				.put(PASS, password)
				.put(HBM2DDL_AUTO, "create-drop")
				.put(CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, true)
				.put(AUTOCOMMIT, false)
                .put(SHOW_SQL, true)
                .put(QUERY_STARTUP_CHECKING, false)
                .put(GENERATE_STATISTICS, false)
                .put(USE_REFLECTION_OPTIMIZER, false)
                .put(USE_SECOND_LEVEL_CACHE, false)
                .put(USE_QUERY_CACHE, false)
                .put(USE_STRUCTURED_CACHE, false)
                .put(STATEMENT_BATCH_SIZE, 20)
                .build()
            );
            } catch(Exception e) {
                log.log(Level.SEVERE, "Unable to initialize persistence", e);
                throw new RuntimeException(e);
            }
            this.entityManagerThreadLocal = new ThreadLocal<>();
    }

	public static EntityManagerHolder createEntityManager() {
        if(instance != null) {
            return instance.internalCreateEntityManager();
        }
        return null;
    }

	private EntityManagerHolder internalCreateEntityManager() {
        boolean created = false;
        EntityManager entityManager = entityManagerThreadLocal.get();
        if(entityManager == null) {
            entityManager = entityManagerFactory.createEntityManager();
            entityManagerThreadLocal.set(entityManager);
            if(entityManager != null) {
                created = true;
            }
        }

		return new EntityManagerHolder(entityManager, created);
	}

	public static boolean isRepository(Class<?> aClass) {
        return instance.repositoryClasses.get(aClass) != null;
	}

	public static Object createRepository(Class<?> aClass) {
		return Proxy.newProxyInstance(
			aClass.getClassLoader(), 
			new Class<?>[] {aClass},
			new RepositoryInvocationHandler(instance.repositoryClasses.get(aClass).value()));
	}

}

