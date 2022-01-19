# nnsfw (no nonsense server framework)
## Features
- web server
- controllers (api) defined via annotations, resources via the resource folder
- basic authentication using openId
- services injected via dependency injection
- basic jpa support, data object and mapping defined via annotations
- transaction support in service calls

## Create an application
The application class is the starting point for your project. Here you will start defining the configuration for all aspects of your project.
Lets start small with an application that starts a webserver:

```
import net.nnwsf.ApplicationServer;
import net.nnwsf.configuration.ServerConfiguration;

@ServerConfiguration(hostname="lvh.me", port=8080)
public class Application {
    public static void main(String[] args) {
         ApplicationServer.start(Application.class);
    }
}
```
This class defines, that the server will listening on lvh.me(localhost) at port 8080. That's all you need to start up the webserver. The configuration can be also read from a configuration file application.yaml.
In this case you only have to add the annotation ```@ServerConfiguration``` without parameters and have the application.yaml contain the following:
```
server:
  hostname: lvh.me
  port: 8080
```
Both configuration options are valid. The configuration file will override the configuration in the annotation.

## Annotations
There are quite a few annotations that will help with dependency injections, setting up controller endpoints and data source.
To minimize the number of files/folder that have to be parsed and speed up the startup, you should specify the root package for your code.
You can do that by adding an annotation to the Application:
```@AnnotationConfiguration("net.example")```
This will limit the annotation lookup to this package and it's sub-packages.
### Package net.nnwsf.application.annotation.* / Application configuration
These annotations have be applied to the application class, more precisely the class that is passed as the parameter when the application server is started.
- AnnotationConfiguration - defines the root package for annotation lookup
- AuthenticatedResourcePathConfiguration - defines the relative root path for resources only authenticated users have access to
- AuthenticationProviderConfiguration - defines the properties that are being used to authenticate users
  - jsonFileName: relative path and file name for the jsonFile that contains authentication configuration
  - callbackPath: path that has been registered with the authentication provider for callback after the user has been authenticated
  - openIdDiscoveryUri: uri that will be used to discover the user profile
- DatasourceConfiguration - defines the data source for accessing a database
  - name: name of the data source, the name will be used in other annotations to reference the data source
  - providerClass: class name of the JPA persistence provider, e.g. org.hibernate.jpa.HibernatePersistenceProvider
  - jdbcDriver: class name of the JDBC driver
  - jdbcUrl: jdbc url for your database
  - schema: database schema
  - user: database user
  - password: database password
  - properties: additional properties
- FlywayConfiguration - see https://flywaydb.org/ for more information
  - datasource: name of the data source
  - location: relative path of the flyway script folder
- ServerConfiguration
  - port: the port the server listens on
  - hostname: the hostname the server listens on
  - resourcePath: relative path for the static resources

### Package net.nnwsf.controller.annotation / Define Controllers and Endpoints
The annotations in this package help defining REST endpoints.

Class annotations:
- Controller - defines that the class contains endpoints that can be called and specifies the common path for all the endpoints in the class
- Get - defines that this method will be called on an HTTP GET request, the value will be appended to the path defined by the Controller annotation 

Method annotations
The value defined for the annotations define the path (appended to the controller path) and can contain path variables. The convention for path variables is ```{variable name}```. 
You can also define request parameters.

Example URL http://lhv.me/bar?var2=foo:
```    
@Controller("/")
public class ExampleController {
    @Get("/{var1}")
    public String getRequest(
      @PathVariable("var1") String echo, 
      @RequestParameter("var2")) {
        return var1 + ":" + var2;
    }
}
```
The values for the parameters: var1 = bar, var2 = foo
Here are the annotations for the supported HTTP methods: 
- Get
- Put 
- Post
- Delete

Here are the supported types of parameters for the methods
- PathVariable
- RequestParameter
- RequestBody - the body of the http request
- AuthenticatedUser - the user for authenticated requests

### Package net.nnwsf.service.annotation / Define Services
The annotations in this package help defining services.

- Service - defines that the class contains a service that can be injected and used in controllers or other services. The Java class can be either an interface or a class. If it's an interface there needs to be one implementation

Example:
```
@Service
public interface ExampleService {
    String echo(String echo);
}

public class ExampleServiceImpl implements ExampleService {
    String echo(String echo) {
      return echo;
    }
}
```
You can now use the service in a controller by using the @Inject annotation and the implementation wil be determined and injected into the controller,
```    
@Controller("/test")
public class ExampleController {

    @Inject
    private ExampleService service;

    @Get("/{var1}")
    public String getRequest(
      @PathVariable("var1") String echo, 
      @RequestParameter("var2")) {
        return service.echo(echo);
    }
}
```

### Package net.nnwsf.persistence.annotation / Define Entities and Repositories
Entities are defined using the default JPA annotations (like @Entity and @Table).
You can access an Entity with a Repository. Every main entity will have it's own repository. To create a repository you will have to define an interface that implements  PersistenceRepository<{Entity Class}, {Primary Key Class}> and annotate it with @Repository.
The framework will supply the implementation for all the operations. Basic operations like findAll, findById, save and delete are there by default. You can add additional finder method definitions by annotating them with @Query.
- Repository - defines the repository for the given entity.
- Query - defines the query for the finder method
- Query - defines parameters for the query

Example:
```
@Entity
@Table(name="test")
public class TestEntity {
    
    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="name")
    private String name;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

@Repository(entityClass = ExampleEntity.class)
public interface ExampleRepository extends PersistenceRepository<ExampleEntity, Integer>{
    @Query("select e from ExampleEntity e")
    Collection<ExampleEntity> getAllWithQuery();

    @Query("select e from ExampleEntity e where e.id = :id")
    ExampleEntity findById(@QueryParameter("id") Integer id);
}
```
Repositories can be injected into services or (not recommended) controllers.
```
    @Inject
    private ExampleRepository repository;
```

### Flyway integration
The Flyway integration is very straightforward. With the annotation @FlywayConfiguration you can defined the datasource that it will use and where to to find the flyway files to run. For the files you need to use the conventions defined here: https://flywaydb.org/documentation/concepts/migrations#sql-based-migrations .
The framework will run flyway to ensure that the database is up to date on every startup.

## Example
Have a look at [example](example/README.md)

## Test
The test coverage is only very basic right now, but the besides the example it it a good place to give you an idea on how to use the framework:

[Configuration](nnsfw/src/test/java/net/nnwsf/configuration/TestConfigurationManager.java)

[Services](nnsfw/src/test/java/net/nnwsf/service/TestServiceManager.java)

[Persistence](nnsfw/src/test/java/net/nnwsf/persistence/TestPersistenceManager.java)
