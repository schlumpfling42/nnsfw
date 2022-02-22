package net.nnwsf.application.annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.nnwsf.configuration.Default;
import net.nnwsf.configuration.annotation.ConfigurationKey;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@ConfigurationKey("datasource")
@Repeatable(DatasourceConfigurations.class)
public @interface DatasourceConfiguration {
    @ConfigurationKey(value = "name", containsKeys = true)
    String name() default Default.DATASOURCE_NAME;
    @ConfigurationKey("${name}.jdbcDriver")
    String jdbcDriver() default "";
    @ConfigurationKey("${name}.jdbcUrl")
    String jdbcUrl() default "";
    @ConfigurationKey("${name}.schema")
    String schema() default "";
    @ConfigurationKey("${name}.user")
    String user() default "";
    @ConfigurationKey("${name}.password")
    String password() default "";
    @ConfigurationKey("${name}.connection.min")
    int minConnections() default 5;
    @ConfigurationKey("${name}.connection.max")
    int maxConnections() default 20;
    @ConfigurationKey("${name}.properties")
    Property[] properties() default {};
}
