package org.keycloak.authentication.adaptive;

import org.keycloak.adaptive.AdaptiveAuthnContext;
import org.keycloak.adaptive.factors.UserContext;
import org.keycloak.adaptive.levels.RiskLevel;
import org.keycloak.models.KeycloakSession;

import java.util.Collections;
import java.util.Set;

public class DefaultAdaptiveAuthnContext implements AdaptiveAuthnContext {
    private final KeycloakSession session;

    private Double riskValue;
    private Set<UserContext> riskFactors = Collections.emptySet();
    private RiskLevel currentRiskLevel;

    public DefaultAdaptiveAuthnContext(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Double getRiskValue() {
        return riskValue;
    }

    @Override
    public void setRiskValue(Double riskValue) {
        this.riskValue = riskValue;
    }

    @Override
    public Set<UserContext> getUsedRiskFactors() {
        return riskFactors;
    }

    @Override
    public void setUsedRiskFactors(Set<UserContext> riskFactors) {
        this.riskFactors = riskFactors;
    }

    @Override
    public RiskLevel getCurrentRiskLevel() {
        return currentRiskLevel;
    }

    @Override
    public void setCurrentRiskLevel(RiskLevel level) {
        this.currentRiskLevel = level;
    }
}
