package org.keycloak.authentication.adaptive.factors.agent;

import org.keycloak.Config;
import org.keycloak.adaptive.factors.UserContextFactory;
import org.keycloak.adaptive.factors.UserContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class HeaderUserAgentContextFactory implements UserContextFactory<UserContext<UserAgent>> {
    public static final String PROVIDER_ID = "default-user-agent-risk-factor";

    @Override
    public UserContext<UserAgent> create(KeycloakSession session) {
        return new HeaderUserAgentContext(session);
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
        return PROVIDER_ID;
    }
}
