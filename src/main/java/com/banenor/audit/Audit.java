package com.banenor.audit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {
    /**
     * Describes the action being audited.
     */
    String action() default "";

    /**
     * Identifies the resource impacted by the action.
     */
    String resource() default "";
}
