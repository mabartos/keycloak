package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.FeatureOptions;
import org.keycloak.config.Option;

import java.util.Map;
import java.util.function.Consumer;

public class FeaturePropertyMappers2 implements PropertyMappersCategory {

    @Override
    public Map<Option<?>, Consumer<PropertyMapper.Builder<?>>> getMappers() {
        return Map.of(
                FeatureOptions.FEATURES, (mapper) -> mapper
                        .paramLabel("feature")
                        .validator(FeaturePropertyMappers::validateEnabledFeature)
        );
    }
}
