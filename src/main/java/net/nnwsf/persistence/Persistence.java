package net.nnwsf.persistence;

import net.nnwsf.util.ClassDiscovery;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.net.URL;
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

import org.hibernate.dialect.PostgreSQL10Dialect;

import static org.hibernate.cfg.AvailableSettings.*;

public class Persistence {

    private static Logger log = Logger.getLogger(Persistence.class.getName());

    private static Persistence instance;

    public static Persistence init(
        Class<? extends PersistenceProvider> persistenceProviderClass,
        String jdbcDriver,
        String jdbcUrl,
        String user,
        String password) {
        if(instance == null) {
            instance = new Persistence(
                persistenceProviderClass,
                jdbcDriver,
                jdbcUrl,
                user,
                password
            );
        }
        return instance;
    }

    private EntityManagerFactory entityManagerFactory;

    public Persistence(
        Class<? extends PersistenceProvider> persistenceProviderClass,
        String jdbcDriver,
        String jdbcUrl,
        String user,
        String password) {

            try {

            this.entityManagerFactory = persistenceProviderClass.newInstance().createContainerEntityManagerFactory(
                new PersistenceUnitInfo(){
                
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
                        return "pu";
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
                },
                ImmutableMap.<String, Object>builder()
                .put(JPA_JDBC_DRIVER, jdbcDriver)
                .put(JPA_JDBC_URL, jdbcUrl)
                .put(DIALECT, PostgreSQL10Dialect.class)
                .put(USER, user)
                .put(PASS, password)
                .put(SHOW_SQL, false)
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
                log.log(Level.SEVERE, "Unable to initialze persistence", e);
                throw new RuntimeException(e);
            }
    }

	public static EntityManager createEntityManager() {
        if(instance != null) {
            return instance.entityManagerFactory.createEntityManager();
        }
        return null;
	}

	private EntityManager internalCreateEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

}

