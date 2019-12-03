package net.nnwsf.configuration;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Server {
    int port() default Integer.MIN_VALUE;
    String hostname() default "";
    String resourcePath() default "";
}
