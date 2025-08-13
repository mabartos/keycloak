package org.keycloak.testsuite.admin.client;

import org.keycloak.Config;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.admin.api.client.ClientsApiFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class TestClientsApiFactory implements ClientsApiFactory {
    public static final String PROVIDER_ID = "test";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public ClientsApi create(KeycloakSession session) {
        return new TestClientsApi();
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }
}
