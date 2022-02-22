package net.example;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Persistence;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.MetadataSource;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.provider.ReactiveServiceRegistryBuilder;
import org.hibernate.reactive.vertx.VertxInstance;
import org.testcontainers.containers.PostgreSQLContainer;

import io.netty.handler.logging.LogLevel;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import net.example.persistence.ExampleEntity;

public class TestVertx extends AbstractVerticle {

  static public class MyVertx implements VertxInstance {

    private final io.vertx.core.Vertx vertx;
  
    public MyVertx() {
      this.vertx = io.vertx.core.Vertx.vertx();
    }
  
    @Override
    public io.vertx.core.Vertx getVertx() {
      return vertx;
    }
  
  }

    private static final Logger logger = Logger.getLogger(TestVertx.class.getName());
    private Mutiny.SessionFactory emf;  // <1>
  
    @Override
    public Uni<Void> asyncStart() {
  // end::preamble[]
  
      // tag::hr-start[]
      Uni<Void> startHibernate = Uni.createFrom().deferred(() -> {
        var pgPort = config().getInteger("pgPort", 5432);
  
        BootstrapServiceRegistryBuilder bootstrapRegistryBuilder =
        new BootstrapServiceRegistryBuilder();
        // add a custom ClassLoader
        bootstrapRegistryBuilder.applyClassLoader( TestVertx.class.getClassLoader() );
        
        
        BootstrapServiceRegistry bootstrapRegistry = bootstrapRegistryBuilder.build();
        
        StandardServiceRegistry standardRegistry = ReactiveServiceRegistryBuilder
            .forJpa(bootstrapRegistry)
            .applySetting("hibernate.connection.url", "jdbc:postgresql://localhost:" + pgPort + "/postgres")
            .applySetting("hibernate.connection.username", "postgres")
            .applySetting("hibernate.connection.password", "vertx-in-action")
            .applySetting("show_sql", "true")
            .applySetting("hibernate.connection.pool_size", 10)
            .applySetting("javax.persistence.schema-generation.database.action", "drop-and-create")
            .build();
            
        MetadataSources sources = new MetadataSources(standardRegistry);
        sources.addAnnotatedClass(ExampleEntity.class);

        MetadataBuilder metadataBuilder = sources.getMetadataBuilder();

        Metadata metadata = metadataBuilder.build();

        emf = metadata.getSessionFactoryBuilder().build()
          .unwrap(Mutiny.SessionFactory.class);
  
        return Uni.createFrom().voidItem();
      });
  
      startHibernate = vertx.executeBlocking(startHibernate)  // <2>
        .onItem().invoke(() -> logger.info("✅ Hibernate Reactive is ready"));
      // end::hr-start[]
  
      // tag::routing[]
      Router router = Router.router(vertx);
  
      BodyHandler bodyHandler = BodyHandler.create();
      router.post().handler(bodyHandler::handle);
  
      router.get("/example").respond(this::listProducts);
      router.get("/example/:id").respond(this::getProduct);
      router.post("/example").respond(this::createProduct);
      // end::routing[]
  
      // tag::async-start[]
      Uni<HttpServer> startHttpServer = vertx.createHttpServer()
        .requestHandler(router::handle)
        .listen(8080)
        .onItem().invoke(() -> logger.info("✅ HTTP server listening on port 8080"));
  
      return Uni.combine().all().unis(startHibernate, startHttpServer).discardItems();  // <1>
      // end::async-start[]
    }
  
    // tag::crud-methods[]
    private Uni<List<ExampleEntity>> listProducts(RoutingContext ctx) {
      return emf.withSession(session -> session
        .createQuery("select e from ExampleEntity e", ExampleEntity.class)
        .getResultList());
    }
  
    private Uni<ExampleEntity> getProduct(RoutingContext ctx) {
      long id = Long.parseLong(ctx.pathParam("id"));
      return emf.withSession(session -> session
        .find(ExampleEntity.class, id))
        .onItem().ifNull().continueWith(ExampleEntity::new);
    }
  
    private Uni<ExampleEntity> createProduct(RoutingContext ctx) {
      ExampleEntity product = ctx.getBodyAsJson().mapTo(ExampleEntity.class);
      return emf.withSession(session -> session.
        merge(product)
          .call(session::flush));
    }
    // end::crud-methods[]
  
    public static void main(String[] args) {
  
      long startTime = System.currentTimeMillis();
  
      logger.info("🚀 Starting a PostgreSQL container");
  
      // tag::tc-start[]
      PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:11-alpine")
        .withDatabaseName("postgres")
        .withUsername("postgres")
        .withPassword("vertx-in-action");
  
      postgreSQLContainer.start();
      // end::tc-start[]
  
      long tcTime = System.currentTimeMillis();
  
      logger.info("🚀 Starting Vert.x");
  
      // tag::vertx-start[]
      Vertx vertx = Vertx.vertx();
  
      DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject()
        .put("pgPort", postgreSQLContainer.getMappedPort(5432))); // <1>
  
      vertx.deployVerticle(TestVertx::new, options).subscribe().with(  // <2>
        ok -> {
          long vertxTime = System.currentTimeMillis();
          logger.info("Deployment success");
          logger.info("PostgreSQL container started in " + (tcTime - startTime) + "ms");
          logger.info("💡 Vert.x app started in " + (vertxTime - tcTime) + "ms");
        },
        err -> logger.log(Level.SEVERE, "Deployment failure", err));
      // end::vertx-start]
    }
    
}
