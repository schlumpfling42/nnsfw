package net.rka.server.fw;

import net.rka.server.fw.configuration.AnnotationConfiguration;
import net.rka.server.fw.configuration.ServerConfiguration;
import net.rka.server.fw.controller.ExampleController;

@ServerConfiguration()
@AnnotationConfiguration("net.rka.server.fw")
public class Application {
    public static void main(String[] args) {
        Server server = Server.start(Application.class);
        server.getConfiguration().getControllerClasses().add(ExampleController.class);
    }
}