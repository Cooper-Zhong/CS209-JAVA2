package lab11;

import java.lang.annotation.*;

/**
 * @author Cay Horstmann
 * @version 1.00 2004-08-17
 */
@Target(ElementType.METHOD) // usage range
@Retention(RetentionPolicy.RUNTIME) // life time
public @interface ActionListenerFor {
    String source();
}
