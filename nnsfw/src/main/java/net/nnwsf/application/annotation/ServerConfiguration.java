package net.nnwsf.application.annotation;

import java.lang.annotation.*;

import net.nnwsf.configuration.annotation.ConfigurationKey;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@ConfigurationKey("server")
public @interface ServerConfiguration {
    @ConfigurationKey("protocol")
    String protocol() default "http";
    @ConfigurationKey("hostname")
    String hostname() default "";
    @ConfigurationKey("port")
    int port() default Integer.MIN_VALUE;
    @ConfigurationKey("resourcePath")
    String resourcePath() default "";
}
