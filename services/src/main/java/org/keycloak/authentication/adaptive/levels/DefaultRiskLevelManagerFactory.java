package org.keycloak.authentication.adaptive.levels;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.adaptive.levels.RiskLevelManager;
import org.keycloak.adaptive.levels.RiskLevelManagerFactory;

public class DefaultRiskLevelManagerFactory implements RiskLevelManagerFactory {
    public static final String PROVIDER_ID = "default-risk-level-manager";

    @Override
    public RiskLevelManager create(KeycloakSession session) {
        return new DefaultRiskLevelManager(session);
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
