package org.keycloak.admin.api.root;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultAdminRootFactory implements AdminRootFactory {
    public static final String PROVIDER_ID = "default";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public AdminRoot create(KeycloakSession session) {
        return new DefaultAdminRoot();
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
