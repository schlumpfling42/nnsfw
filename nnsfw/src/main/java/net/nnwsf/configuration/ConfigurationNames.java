package net.nnwsf.configuration;

public interface ConfigurationNames {
    String APPLICATION_HOSTNAME = "${application.hostname}";
    String APPLICATION_PORT = "${application.port}";
    String APPLICATION_RESOURCEPATH = "${application.resourcePath}";

    String DATASOURCE_PROVIDERCLASS = "${datasource.providerClass}";
    String DATASOURCE_JDBCDRIVER = "${datasource.jdbcDriver}";
    String DATASOURCE_JDBCURL = "${datasource.jdbcUrl}";
    String DATASOURCE_USERNAME = "${datasource.user}";
    String DATASOURCE_PASSWORD = "${datasource.password}";
    String DATASOURCE_PROPERTIES = "${datasource.properties}";
}
