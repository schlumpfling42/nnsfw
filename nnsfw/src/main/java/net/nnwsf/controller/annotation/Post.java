package net.nnwsf.controller.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Post {
    String value() default "/";
    String contentType() default "application/json";
}
