package org.keycloak.admin.api;

import org.keycloak.provider.Provider;

public interface AdminApi extends Provider {

    @Override
    default void close() {
    }
}
