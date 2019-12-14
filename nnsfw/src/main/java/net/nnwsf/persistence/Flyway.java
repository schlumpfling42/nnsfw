package net.nnwsf.persistence;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.nnwsf.configuration.ConfigurationKey;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@ConfigurationKey("flyway")
public @interface Flyway {
    @ConfigurationKey("location")
    String location() default "";
}
