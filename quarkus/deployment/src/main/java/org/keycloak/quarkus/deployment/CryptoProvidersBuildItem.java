package org.keycloak.quarkus.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import org.keycloak.common.crypto.FipsMode;

import java.util.Map;

public final class CryptoProvidersBuildItem extends SimpleBuildItem {
    private final Map<FipsMode, Class<?>> providers;

    public CryptoProvidersBuildItem(Map<FipsMode, Class<?>> providers) {
        this.providers = providers;
    }

    public Map<FipsMode, Class<?>> getProviders() {
        return providers;
    }
}
