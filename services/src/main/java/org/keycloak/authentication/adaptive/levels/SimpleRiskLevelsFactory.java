package org.keycloak.authentication.adaptive.levels;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.adaptive.levels.RiskLevels;
import org.keycloak.adaptive.levels.RiskLevelsFactory;

public class SimpleRiskLevelsFactory implements RiskLevelsFactory {
    public static final String PROVIDER_ID = "simple-risk-levels";
    private static final RiskLevels defaultRiskLevels = new SimpleRiskLevels();

    @Override
    public RiskLevels create(KeycloakSession session) {
        return defaultRiskLevels;
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
