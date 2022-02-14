# nnsfw (no nonsense server framework) - nocode feature
## Features
- web server
- api defined via json-schema resources in the resource folder

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

## Add nocode configuration
To add nocode configuration, simply add a datasource and a configuration with the location of the schema files.
The application class should look like that:
```
import net.nnwsf.ApplicationServer;
import net.nnwsf.configuration.ServerConfiguration;

@DatasourceConfiguration(name = "nocode")
@NocodeConfiguration(datasource = "nocode", schemas = {"/nocode/category.json", "/nocode/product.json"}, controllerPath = "/nocode")
@ServerConfiguration(hostname="lvh.me", port=8080)
public class Application {
    public static void main(String[] args) {
         ApplicationServer.start(Application.class);
    }
}
```

Now define the schema of your domain objects. Here is an example: category.json:
```
{
    "$schema": "http://json-schema.org/draft-04/schema#",
    "title": "Category",
    "description": "A product from Acme's catalog",
    "type": "object",
    "properties": {
        "id": {
            "description": "The unique identifier for a product",
            "type": "integer"
        },
        "name": {
            "description": "Name of the category",
            "type": "string"
        }
    },
    "required": ["id", "name"]
}
```
The nocode implementation will create get, delete, post and post endpoints for the entity "category" (as defined byt the title attribute) at localhost:8080/nocode/category. It will also create a database table with the same name.

Here is a more complex schema: product.json:
```
{
    "$schema": "http://json-schema.org/draft-04/schema#",
    "title": "Product",
    "description": "A product from Acme's catalog",
    "type": "object",
    "properties": {
        "id": {
            "description": "The unique identifier for a product",
            "type": "integer"
        },
        "name": {
            "description": "Name of the product",
            "type": "string"
        },
        "count": {
            "description": "Number in stock",
            "type": "integer"
        },
        "price": {
            "description": "Price",
            "type": "double"
        },
        "isAvailable": {
            "description": "Availability",
            "type": "boolean"
        },
        "addedDate": {
            "description": "Date the product was added",
            "type": "date"
        },
        "updatedDate": {
            "description": "Date the product was updated",
            "type": "date"
        },
        "category": {
            "$ref": "Category"
        },
        "subProducts": {
            "type": "array",
            "items": {
                "title": "SubProduct",
                "description": "A sub product from Acme's catalog",
                "type": "object",
                "properties": {
                    "id": {
                        "description": "The unique identifier for a category",
                        "type": "integer"
                    },
                    "name": {
                        "description": "Name of the category",
                        "type": "string"
                    }
                }
            }
        }
    },
    "required": ["id", "name"]
}
```