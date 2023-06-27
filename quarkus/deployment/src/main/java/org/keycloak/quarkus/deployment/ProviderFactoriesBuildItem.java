package org.keycloak.quarkus.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

import java.util.Map;

public final class ProviderFactoriesBuildItem extends SimpleBuildItem {

    private final Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories;
    private final Map<Class<? extends Provider>, String> defaultProviders;
    private final Map<String, ProviderFactory> preConfiguredProviders;

    public ProviderFactoriesBuildItem(Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories,
                                      Map<Class<? extends Provider>, String> defaultProviders,
                                      Map<String, ProviderFactory> preConfiguredProviders) {
        this.factories = factories;
        this.defaultProviders = defaultProviders;
        this.preConfiguredProviders = preConfiguredProviders;
    }

    public Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> getFactories() {
        return factories;
    }

    public Map<Class<? extends Provider>, String> getDefaultProviders() {
        return defaultProviders;
    }

    public Map<String, ProviderFactory> getPreConfiguredProviders() {
        return preConfiguredProviders;
    }
}
