package net.nnwsf.nocode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.nnwsf.application.annotation.DatasourceConfiguration;
import net.nnwsf.application.annotation.NocodeConfiguration;
import net.nnwsf.persistence.DatasourceManager;
import net.nnwsf.persistence.PersistenceRepository;
import net.nnwsf.persistence.annotation.Repository;
import net.nnwsf.util.Pair;
import net.nnwsf.util.ResourceUtil;

public class NocodeManager {

    private static NocodeManager instance;

    public static void init(ClassLoader classLoader, NocodeConfiguration nocodeConfiguration) {
        instance = new NocodeManager(classLoader, nocodeConfiguration);
    }

    public static Map<Class<PersistenceRepository>, Repository> getPersistenceClasses() {
        return instance.entityRepositoryMap;
    }
    public static Pair<Class<? extends NocodeEntity>, Class<?>> getEntityClass(SchemaObject schemaObject) {
        return instance.entityIdClasses.get(schemaObject);
    }

    public static Collection<Class<?>> getEntityClasses() {
        return instance.internalGetEntityClasses();
    }
    
    public static Class<PersistenceRepository> getRepositoryClass(SchemaObject schemaObject) {
        return instance.respositoryClasses.get(schemaObject);
    }
    
    public static Collection<SchemaObject> getSchemas() {
        return instance.schemas;
    }
    
    public static String getControllerPath() {
        return instance.controllerPath;
    }

    public static ClassLoader getClassLoader() {
        return instance.classLoader;
    }

    private final String controllerPath;
    private final Collection<SchemaObject> schemas;
    private final Map<SchemaObject, Pair<Class<? extends NocodeEntity>, Class<?>>> entityIdClasses;
    private final Map<Class<? extends NocodeEntity>, Collection<Class<? extends NocodeEntity>>> entityDependencies;
    private final Map<SchemaObject, Class<PersistenceRepository>> respositoryClasses;
    private final Map<Class<PersistenceRepository>, Repository> entityRepositoryMap;
    private ClassLoader classLoader;
    private final DatasourceConfiguration datasourceConfiguration;

    NocodeManager(ClassLoader classLoader, NocodeConfiguration nocodeConfiguration) {
        this.schemas = new ArrayList<>();
        this.entityIdClasses = new HashMap<>();
        this.entityDependencies = new HashMap<>();
        this.respositoryClasses = new HashMap<>();
        this.entityRepositoryMap = new HashMap<>();
        if(nocodeConfiguration != null) {
            this.datasourceConfiguration = DatasourceManager.getDatasourceConfiguration(nocodeConfiguration.datasource());
            controllerPath = nocodeConfiguration.controllerPath();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Arrays.stream(nocodeConfiguration.schemas())
            .forEach(aSchemaLocation -> {
                try {
                    SchemaObject schemaElement = objectMapper.readValue(ResourceUtil.getResourceAsString(classLoader, aSchemaLocation), SchemaObject.class);
                    schemas.add(schemaElement);
                    Pair<Class<? extends NocodeEntity>, Class<?>> entityIdClassPair = createEntityClass(schemaElement.getTitle(), schemaElement);
                    entityIdClasses.put(schemaElement, entityIdClassPair);
                    if(this.classLoader == null) {
                        this.classLoader = entityIdClassPair.getFirst().getClassLoader();
                    }
                    Class<PersistenceRepository> entityRepositoryClass = (Class<PersistenceRepository>)createEntityRepositoryClass(entityIdClassPair);
                    respositoryClasses.put(schemaElement, entityRepositoryClass);
                    Repository repository = new Repository() {
                        public Class entityClass() {
                            return entityIdClassPair.getFirst();
                        }
        
                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return Repository.class;
                        }
        
                        @Override
                        public String datasource() {
                            return datasourceConfiguration.name();
                        }
                    };
                    entityRepositoryMap.put(entityRepositoryClass, repository);
                } catch(Exception e) {
                    throw new RuntimeException("Unable to load nocode schema: " + aSchemaLocation, e);
                }
                createTables();
            });
            if(this.classLoader == null) {
                this.classLoader = classLoader;
            }
        } else {
            this.controllerPath = null;
            this.datasourceConfiguration = null;
        }

    }

    private Pair<Class<? extends NocodeEntity>, Class<?>> createEntityClass(String name, SchemaObject schemaObject) {
        try {
            Collection<Class<? extends NocodeEntity>> dependentClasses = new ArrayList<>();
            Class<?>[] idClass = new Class[1];
            Builder<? extends NocodeEntity> builder = new ByteBuddy().subclass(NocodeEntity.class);
            builder = builder.annotateType(AnnotationDescription.Builder.ofType(Table.class)
                .define("name", name).build());
            builder = builder.annotateType(AnnotationDescription.Builder.ofType(Entity.class).build());
            for(Entry<String, SchemaElement> anEntry: schemaObject.getProperties().entrySet()) {
                SchemaElement schemaElement = anEntry.getValue();
                String attributeName = anEntry.getKey();
                if("id".equalsIgnoreCase(attributeName)) {
                    idClass[0] = getType(schemaElement);
                    builder = builder
                        .defineField(attributeName, getType(schemaElement), Modifier.PUBLIC)
                        .annotateField(AnnotationDescription.Builder.ofType(Id.class)
                            .build())
                        .annotateField(AnnotationDescription.Builder.ofType(GeneratedValue.class)
                        .define("strategy", GenerationType.IDENTITY)
                        .build());
                } else {
                    if(schemaElement instanceof SchemaPrimitive) {
                        builder = builder
                            .defineField(attributeName, getType(schemaElement), Modifier.PUBLIC)
                            .annotateField(AnnotationDescription.Builder.ofType(Column.class)
                                .define("name", attributeName)
                                .build());
                    } else if(schemaElement instanceof SchemaObject) {
                        Pair<Class<? extends NocodeEntity>, Class<?>> attributeClass = createEntityClass(attributeName, (SchemaObject)schemaElement);
                        dependentClasses.add(attributeClass.getFirst());
                        builder = builder
                            .defineField(attributeName, attributeClass.getFirst(), Modifier.PUBLIC)
                            .annotateField(AnnotationDescription.Builder.ofType(ManyToOne.class)
                                .defineEnumerationArray("cascade", CascadeType.class, CascadeType.ALL)
                                .build())
                            .annotateField(AnnotationDescription.Builder.ofType(JoinColumn.class)
                                .define("name", attributeName + "_id")
                                .define("nullable", true)
                                .build());
                    }
                }
            }
            Class<? extends NocodeEntity> entityClass = builder.make().load(NocodeEntity.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION).getLoaded();
            entityDependencies.put(entityClass, dependentClasses);
            return Pair.of(entityClass, idClass[0]);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Collection<Class<?>> internalGetEntityClasses() {
        List<Class<?>> classes = new ArrayList<>();
        entityDependencies.entrySet().forEach(anEntry -> {
            anEntry.getValue().forEach(aClass -> classes.add(aClass));
            classes.add(anEntry.getKey());
        });
        return classes;
    }

    private Class<PersistenceRepository> createEntityRepositoryClass(Pair<Class<? extends NocodeEntity>, Class<?>> entityIdClassPair) {
        Builder<?> builder = new ByteBuddy()
            .makeInterface(TypeDescription.Generic.Builder.parameterizedType(PersistenceRepository.class, entityIdClassPair.getFirst(), entityIdClassPair.getSecond()).build());

        return (Class<PersistenceRepository>) builder.make().load(entityIdClassPair.getFirst().getClassLoader()).getLoaded();
    }

    private Class<?> getType(SchemaElement element) {
        if(SchemaPrimitive.class.isInstance(element)) {
            switch(((SchemaPrimitive)element).getType().toLowerCase()) {
                case "string":
                    return String.class;
                case "integer":
                    return Integer.class;
                case "short":
                    return Integer.class;
                case "long":
                    return Long.class;
                case "float":
                    return Float.class;
                case "double":
                    return Double.class;
                case "char":
                    return Character.class;
                case "byte":
                    return Byte.class;
                case "boolean":
                    return Boolean.class;
                case "date":
                    return Date.class;
                case "timestamp":
                    return Timestamp.class;
                default:
                    throw new RuntimeException("Unsupported type: " + ((SchemaPrimitive)element).getType());
            }
        }
        return Object.class;
    }

    private void createTables() {

        Map<String, String> properties = Optional.ofNullable(datasourceConfiguration.properties())
        .map(p -> 
            Arrays.stream(p)
            .collect(Collectors.toMap(v -> v.name(), v -> v.value()))
        ).orElse(new HashMap<>());


        BootstrapServiceRegistryBuilder bootstrapRegistryBuilder =
        new BootstrapServiceRegistryBuilder();
        // add a custom ClassLoader
        bootstrapRegistryBuilder.applyClassLoader( classLoader );

        BootstrapServiceRegistry bootstrapRegistry = bootstrapRegistryBuilder.build();

        StandardServiceRegistry standardRegistry = StandardServiceRegistryBuilder
            .forJpa(bootstrapRegistry)
            .applySetting("connection.driver_class", datasourceConfiguration.jdbcDriver())
            .applySetting("hibernate.connection.url", datasourceConfiguration.jdbcUrl())
            .applySetting("hibernate.connection.username", datasourceConfiguration.user() == null ? "" : datasourceConfiguration.user())
            .applySetting("hibernate.connection.password", datasourceConfiguration.password() == null ? "" : datasourceConfiguration.password())
            .applySetting("hibernate.hbm2ddl.auto", "update")
            .applySetting("show_sql", "true")
            .applySettings(properties)
            .build();

        MetadataSources sources = new MetadataSources(standardRegistry);
        internalGetEntityClasses().forEach(aClass -> sources.addAnnotatedClass(aClass));

        MetadataBuilder metadataBuilder = sources.getMetadataBuilder();

        Thread.currentThread().setContextClassLoader(classLoader);

        SchemaUpdate schemaUpdate = new SchemaUpdate();
        schemaUpdate.setHaltOnError(false);
        schemaUpdate.setFormat(true);
        schemaUpdate.setDelimiter(";");
        schemaUpdate.execute(EnumSet.of(TargetType.DATABASE), metadataBuilder.build());
    }
}
