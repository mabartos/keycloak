package org.keycloak.admin.api.client;

import org.keycloak.models.KeycloakSession;

public class ClientsApiV2Factory implements ClientsApiFactory {
    @Override
    public String getId() {
        return getDefaultProviderId();
    }

    @Override
    public ClientsApi create(KeycloakSession session) {
        return new ClientsApiV2(session);
    }
}
