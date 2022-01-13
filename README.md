# nnsfw (no nonsense server framework)
## Features
- web server
- controllers (api) defined via annotations, resources via the resource folder
- basic authentication using openId
- services injected via dependency injection
- basic jpa support, data object and mapping defined via annotations4
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
This class defines, that the webservce will start on the lvh.me(localhost) at port 8080. That's all you need to start up the webserver. The configuration can be also read from a confgiuration file application.yaml.
In this case you only have to add the annotation ```@ServerConfiguration``` without parameters and have the application.yaml contain the following:
```
server:
  hostname: lvh.me
  port: 8080
```
Both configuration options are valid.

## Example
Have a look at [example](example/README.md)

## Test
