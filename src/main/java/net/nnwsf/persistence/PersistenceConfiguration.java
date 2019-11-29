package net.nnwsf.persistence;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.persistence.spi.PersistenceProvider;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface PersistenceConfiguration {
    Class<? extends PersistenceProvider> providerClass();
    String jdbcDriver();
    String jdbcUrl();
    String dialect();
    String user();
    String password();
}
