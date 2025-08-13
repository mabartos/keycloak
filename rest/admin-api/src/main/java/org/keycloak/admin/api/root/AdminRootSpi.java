package org.keycloak.admin.api.root;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class AdminRootSpi implements Spi {
    public static final String NAME = "admin-api-root";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return AdminRoot.class;
    }

    @Override
    public Class<? extends ProviderFactory<AdminRoot>> getProviderFactoryClass() {
        return AdminRootFactory.class;
    }

    @Override
    public boolean isInternal() {
        return true;
    }
}
