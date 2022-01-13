package net.nnwsf.configuration;

import java.lang.annotation.*;

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
