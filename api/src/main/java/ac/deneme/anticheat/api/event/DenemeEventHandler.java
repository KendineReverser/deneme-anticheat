package ac.deneme.anticheat.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DenemeEventHandler {
    /** Listener priority. Defaults to {@link ListenerPriority#NORMAL}. */
    int priority() default ListenerPriority.NORMAL;
    boolean ignoreCancelled() default false;
}
