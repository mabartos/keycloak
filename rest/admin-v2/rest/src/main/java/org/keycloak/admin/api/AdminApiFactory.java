package org.keycloak.admin.api;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderFactory;

public interface AdminApiFactory<T extends AdminApi> extends ProviderFactory<T>, EnvironmentDependentProviderFactory {

    default String getDefaultProviderId() {
        return "default-v" + getVersion();
    }

    default int getVersion() {
        return 2;
    }

    @Override
    default boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_ADMIN_API_V2); // There's currently only Client API for the new Admin API v2
    }

    default void init(Config.Scope config) {
    }

    default void postInit(KeycloakSessionFactory factory) {
    }

    default void close() {
    }
}
