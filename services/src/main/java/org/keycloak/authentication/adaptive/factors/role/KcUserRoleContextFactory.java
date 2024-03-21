package org.keycloak.authentication.adaptive.factors.role;

import org.keycloak.Config;
import org.keycloak.adaptive.factors.UserContextFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class KcUserRoleContextFactory implements UserContextFactory<UserRoleContext> {

    @Override
    public UserRoleContext create(KeycloakSession session) {
        return null;
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

    @Override
    public String getId() {
        return null;
    }
}
