package org.biomart.processors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FieldInfo {
    String displayName() default "";
    String defaultValue() default "";
    boolean required() default false;
    boolean clientDefined() default false;
}

