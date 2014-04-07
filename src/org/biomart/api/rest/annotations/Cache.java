package org.biomart.api.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author jhsu
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Cache {
    int maxAge() default -1;
    boolean noCache() default false;
    boolean noStore() default false;
    boolean noTransform() default true;
    boolean mustRevalidate() default false;
    boolean proxyRevalidate() default false;
}
