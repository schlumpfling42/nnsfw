package net.nnwsf.controller.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
    String value() default "/";
}
