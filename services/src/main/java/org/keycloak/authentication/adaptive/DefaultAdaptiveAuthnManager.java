package org.keycloak.authentication.adaptive;

import org.keycloak.authentication.adaptive.engines.DefaultRiskEngineFactory;
import org.keycloak.authentication.adaptive.levels.DefaultRiskLevelManagerFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.adaptive.manager.AdaptiveAuthnManager;
import org.keycloak.adaptive.engines.RiskEngine;
import org.keycloak.adaptive.levels.RiskLevelManager;

public class DefaultAdaptiveAuthnManager implements AdaptiveAuthnManager {
    private final KeycloakSession session;
    private final RiskEngine riskEngine;
    private final RiskLevelManager riskLevelManager;

    public DefaultAdaptiveAuthnManager(KeycloakSession session) {
        this.session = session;
        this.riskEngine = session.getProvider(RiskEngine.class, DefaultRiskEngineFactory.PROVIDER_ID);
        this.riskLevelManager = session.getProvider(RiskLevelManager.class, DefaultRiskLevelManagerFactory.PROVIDER_ID);
    }

    @Override
    public RiskEngine getRiskEngine() {
        return riskEngine;
    }

    @Override
    public RiskLevelManager getRiskLevelManager() {
        return riskLevelManager;
    }

    @Override
    public void close() {

    }
}
