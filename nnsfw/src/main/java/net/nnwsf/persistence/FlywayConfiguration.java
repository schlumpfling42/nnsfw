package net.nnwsf.persistence;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.nnwsf.configuration.ConfigurationKey;
import net.nnwsf.configuration.Default;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@ConfigurationKey("flyway")
public @interface FlywayConfiguration {
    @ConfigurationKey(value = "datasource", containsKeys = true)
    String datasource() default Default.DATASOURCE_NAME;
    @ConfigurationKey("${datasource}.location")
    String location() default "";
}
