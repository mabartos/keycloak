package org.keycloak.it.junit5.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LaunchOptimizedStart {

    String[] initialParams() default {"start", "--optimized", "--hostname-strict=false", "http-enabled=true"};

    // Additional parameters
    String[] value();
}
