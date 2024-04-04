package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public interface PropertyMappersWrapper {

    List<PropertyMapper<?>> getPropertyMappers();

    default List<PropertyMapperValidator> getValidators() {
        return Collections.emptyList();
    }

    default BooleanSupplier isEnabled() {
        return () -> true;
    }

    default String getEnabledWhen() {
        return "";
    }
}
