package net.nnwsf.configuration;

public class ServerConfigurationImpl implements ServerConfiguration {

    public int getPort() {
        return 8080;
    }
    public String getHostname() {
        return "localhost";
    }

    @Override
    public String getResourcePath() {
        return "/static";
    }

    

}