package net.nnwsf.authentication;

import java.lang.annotation.*;

/**
 * The endpoints needs to have valid authentication credentials. If there is no
 * valid authentication and authenticate is false,
 */
@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Authenticated {
    boolean authenticate() default false;
}
