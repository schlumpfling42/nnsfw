package net.nnwsf.configuration;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Server {
    Class<? extends ServerConfiguration> value() default ServerConfigurationImpl.class;
}
