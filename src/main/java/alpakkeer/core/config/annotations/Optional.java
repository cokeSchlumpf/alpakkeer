package alpakkeer.core.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be attached to the field if the configuration value is optional. No exception
 * will be thrown if not set.
 *
 * @author Michael Wellner (michael_wellner@rcomext.com).
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Optional {
}

