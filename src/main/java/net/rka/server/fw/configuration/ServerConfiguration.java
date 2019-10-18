package net.rka.server.fw.configuration;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface ServerConfiguration {
    Class<? extends ServerConfigurationImpl> value() default ServerConfigurationImpl.class;
}
