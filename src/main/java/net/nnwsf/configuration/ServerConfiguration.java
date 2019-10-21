package net.nnwsf.configuration;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface ServerConfiguration {
    Class<? extends ServerConfigurationImpl> value() default ServerConfigurationImpl.class;
}
