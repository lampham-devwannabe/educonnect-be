package com.sep.educonnect.helper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should have their response automatically localized
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoLocalize {
    /**
     * Whether to localize fields (nameEn/nameVi -> name based on locale)
     */
    boolean localizeFields() default true;
    
    /**
     * Whether to format price fields according to locale
     */
    boolean formatPrices() default true;
}