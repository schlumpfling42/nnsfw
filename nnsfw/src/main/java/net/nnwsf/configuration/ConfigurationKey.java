package net.nnwsf.configuration;

import java.lang.annotation.*;

@Documented
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurationKey {
    String value();
}