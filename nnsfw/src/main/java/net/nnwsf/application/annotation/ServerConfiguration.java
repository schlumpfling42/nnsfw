package net.nnwsf.application.annotation;

import java.lang.annotation.*;

import net.nnwsf.configuration.annotation.ConfigurationKey;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@ConfigurationKey("server")
public @interface ServerConfiguration {
    @ConfigurationKey("port")
    int port() default Integer.MIN_VALUE;
    @ConfigurationKey("hostname")
    String hostname() default "";
    @ConfigurationKey("resourcePath")
    String resourcePath() default "";
}
