package org.keycloak.authentication.adaptive.levels;

import org.keycloak.models.KeycloakSession;
import org.keycloak.adaptive.levels.RiskLevel;
import org.keycloak.adaptive.levels.RiskLevelManager;
import org.keycloak.adaptive.levels.RiskLevels;

import java.util.Map;
import java.util.Set;

public class DefaultRiskLevelManager implements RiskLevelManager {
    private final KeycloakSession session;
    private final Set<RiskLevel> levels;

    public DefaultRiskLevelManager(KeycloakSession session) {
        this.session = session;
        this.levels = session.getProvider(RiskLevels.class, SimpleRiskLevelsFactory.PROVIDER_ID).getRiskLevels();
    }

    @Override
    public void determineLevel() {

    }

    @Override
    public Set<RiskLevel> getLevels() {
        return levels;
    }

    @Override
    public void execute() {

    }

    @Override
    public void close() {

    }
}
