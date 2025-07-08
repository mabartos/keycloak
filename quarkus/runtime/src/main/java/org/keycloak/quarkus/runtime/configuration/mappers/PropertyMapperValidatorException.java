package org.keycloak.quarkus.runtime.configuration.mappers;

public class PropertyMapperValidatorException extends RuntimeException {

    public PropertyMapperValidatorException(String message) {
        super(message);
    }

    public PropertyMapperValidatorException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
