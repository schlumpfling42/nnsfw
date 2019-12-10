package net.nnwsf.persistence;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.persistence.spi.PersistenceProvider;

import net.nnwsf.configuration.ConfigurationKey;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@ConfigurationKey("datasource")
public @interface Datasource {
    @ConfigurationKey("providerClass")
    Class<? extends PersistenceProvider> providerClass() default PersistenceProvider.class;
    @ConfigurationKey("jdbcDriver")
    String jdbcDriver() default "";
    @ConfigurationKey("jdbcUrl")
    String jdbcUrl() default "";
    @ConfigurationKey("user")
    String user() default "";
    @ConfigurationKey("password")
    String password() default "";
    @ConfigurationKey("properties")
    Property[] properties() default {};
}
