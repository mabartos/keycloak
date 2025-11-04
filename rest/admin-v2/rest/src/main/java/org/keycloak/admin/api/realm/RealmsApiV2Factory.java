package org.keycloak.admin.api.realm;

import org.keycloak.models.KeycloakSession;

public class RealmsApiV2Factory implements RealmsApiFactory {
    @Override
    public String getId() {
        return getDefaultProviderId();
    }

    @Override
    public RealmsApi create(KeycloakSession session) {
        return new RealmsApiV2(session);
    }
}
