package org.keycloak.quarkus.runtime.configuration.mappers;

public interface PropertyMapperValidator {
    void validate() throws PropertyMapperValidatorException;
}
