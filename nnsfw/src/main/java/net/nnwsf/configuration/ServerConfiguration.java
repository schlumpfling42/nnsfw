package net.nnwsf.configuration;

import java.lang.annotation.*;

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
