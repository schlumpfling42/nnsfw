package net.nnwsf.controller.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.PARAMETER)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticatedUser {
}
