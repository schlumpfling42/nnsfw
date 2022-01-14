package net.nnwsf.application.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticatedResourcePathConfiguration {
    String value();
}
