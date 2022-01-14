package net.nnwsf.application.annotation;

import java.lang.annotation.*;

import net.nnwsf.configuration.annotation.ConfigurationKey;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@ConfigurationKey("authenticationProvider")
public @interface AuthenticationProviderConfiguration {
    @ConfigurationKey("jsonFileName")
    String jsonFileName() default "";
    @ConfigurationKey("callbackPath")
    String callbackPath() default "";
    @ConfigurationKey("openIdDiscoveryUri")
    String openIdDiscoveryUri() default "";
}
