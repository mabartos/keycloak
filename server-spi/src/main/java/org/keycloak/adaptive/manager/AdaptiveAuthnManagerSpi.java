package org.keycloak.adaptive.manager;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class AdaptiveAuthnManagerSpi implements Spi {
    public static final String SPI_NAME = "adaptive-authn-manager-spi";

    @Override
    public String getName() {
        return SPI_NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return AdaptiveAuthnManager.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return AdaptiveAuthnManagerFactory.class;
    }

    @Override
    public boolean isInternal() {
        return false;
    }
}
