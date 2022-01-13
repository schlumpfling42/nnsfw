# NNSFW Example

A basic example on how to use NNSFW

## What do I need to start it up?

1. The example relies on Google Authentication for securing some endpoints. You can start it up without adding the credentials, but some endpoints will not work,
   In order using secured endpoints you will need to add `credential.json` to `example/src/resources`. If you haven't done it before, please have a look here: https://developers.google.com/workspace/guides/create-credentials .
   Here is how it should look like:

```json
{
  "web": {
    "client_id": <your client id>,
    "project_id": <your project id>,
    "auth_uri": "https://accounts.google.com/o/oauth2/auth",
    "token_uri": "https://oauth2.googleapis.com/token",
    "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
    "client_secret": <your client secret>,
    "redirect_uris": [
      "http://lvh.me:8080/login"
    ],
    "javascript_origins": [
      "http://lvh.me:8080"
    ]
  }
}
```

2. You don't need to set it ut to run the example, but there are a couple of ways to configure the examples. First have a look at [application.yaml](src/main/resources/application.yaml). There you have the datasource for the in-memory database configured. Other configuration elements are done in the code via annotations. If you look at [Application,java](src/main/java/net/example/Application.java) you can see an annotation like `@AnnotationConfiguration("net.example")`. This will help the framework to look for other annotations inside the example.

## Start the example

After you added the credential file, you can start the example by running `net.example.Application` .
The startup should only take a few seconds. The default port the example application listens to is 8080.
To test it out go to the following url: http://lvh.me:8080/index.html
If you have the google credentials setup all the way go to http://lvh.me:8080/secure/index.html
There are a few rest endpoints to try out. They are defined in [ExampleController.java](src/main/java/net/example/controller/ExampleController.java). The base URL for the rest endpoints is http://lvh.me:8080/test . The easiest way to test them out is to use Postman. 

There is also a Rest endpoint that requires authentication http://lvh.me:8080/auth. But you will need a valid access token to try it out. Here is a good example on how to set up Postman to create an access token: https://medium.com/kinandcartacreated/google-authentication-with-postman-12943b63e76a
