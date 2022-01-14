package net.nnwsf.persistence.annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE_PARAMETER)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Property {
    String name();
    String value();
}
