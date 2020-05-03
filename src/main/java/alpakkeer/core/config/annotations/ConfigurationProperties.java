package alpakkeer.core.config.annotations;

import com.typesafe.config.Config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to annotate configuration class for {@link Configs#mapToConfigClass(Class, Config)}.
 *
 * @author Michael Wellner (michael_wellner@rcomext.com).
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurationProperties {

    String prefix() default "";

}