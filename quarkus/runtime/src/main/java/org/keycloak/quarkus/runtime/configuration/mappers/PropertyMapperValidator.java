package org.keycloak.quarkus.runtime.configuration.mappers;

public interface PropertyMapperValidator {

    Runnable getValidation();

    default boolean inRuntime() {
        return true;
    }
}
