package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.Option;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface PropertyMappersCategory {

    Map<Option<?>, Consumer<PropertyMapper.Builder<?>>> getMappers();

    default List<PropertyMapperValidator> getValidators() {
        return Collections.emptyList();
    }

    default boolean isEnabled() {
        return true;
    }
}
