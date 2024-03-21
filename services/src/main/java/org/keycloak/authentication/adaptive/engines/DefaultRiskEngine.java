package org.keycloak.authentication.adaptive.engines;

import org.keycloak.adaptive.engines.RiskEngine;
import org.keycloak.adaptive.factors.UserContext;
import org.keycloak.adaptive.factors.RiskFactorEvaluator;
import org.keycloak.models.KeycloakSession;

import java.util.Set;

public class DefaultRiskEngine implements RiskEngine {
    private final KeycloakSession session;
    private final Set<UserContext> riskFactors;
    private final Set<RiskFactorEvaluator> riskFactorEvaluators;
    private Double riskValue;

    public DefaultRiskEngine(KeycloakSession session) {
        this.session = session;
        this.riskFactors = session.getAllProviders(UserContext.class);
        this.riskFactorEvaluators = session.getAllProviders(RiskFactorEvaluator.class);
    }

    @Override
    public Double getRiskValue() {
        return riskValue;
    }

    @Override
    public Set<UserContext> getRiskFactors() {
        return riskFactors;
    }

    @Override
    public Set<RiskFactorEvaluator> getRiskEvaluators() {
        return riskFactorEvaluators;
    }

    @Override
    public void evaluateRisk() {
        //todo very naive
        this.riskValue = getRiskEvaluators().stream().mapToDouble(RiskFactorEvaluator::getRiskValue).sum();
    }

    @Override
    public void close() {

    }
}
