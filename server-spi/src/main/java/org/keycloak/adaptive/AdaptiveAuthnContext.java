package org.keycloak.adaptive;

import org.keycloak.adaptive.factors.UserContext;
import org.keycloak.adaptive.levels.RiskLevel;

import java.util.Set;

public interface AdaptiveAuthnContext {

    Double getRiskValue();

    void setRiskValue(Double riskValue);

    Set<UserContext> getUsedRiskFactors();

    void setUsedRiskFactors(Set<UserContext> riskFactors);

    RiskLevel getCurrentRiskLevel();

    void setCurrentRiskLevel(RiskLevel level);
}
