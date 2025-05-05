package org.keycloak.quarkus.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

import java.util.Map;

public final class KeycloakFactoriesBuildItem extends SimpleBuildItem {
    Map<Spi, Map<Class<? extends Provider>, Map<String, ProviderFactory>>> factories;
    Map<String, ProviderFactory> preConfiguredFactories;

    public KeycloakFactoriesBuildItem(Map<Spi, Map<Class<? extends Provider>, Map<String, ProviderFactory>>> factories, Map<String, ProviderFactory> preConfiguredFactories) {
        this.factories = factories;
        this.preConfiguredFactories = preConfiguredFactories;
    }

    public Map<Spi, Map<Class<? extends Provider>, Map<String, ProviderFactory>>> getFactories() {
        return factories;
    }

    public Map<String, ProviderFactory> getPreConfiguredFactories() {
        return preConfiguredFactories;
    }

}
