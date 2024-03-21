package org.keycloak.authentication.adaptive.factors.agent;

import org.keycloak.Config;
import org.keycloak.adaptive.RiskWeights;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.adaptive.factors.RiskFactorEvaluator;
import org.keycloak.adaptive.factors.RiskFactorEvaluatorFactory;

public class DefaultUserAgentEvaluatorFactory implements RiskFactorEvaluatorFactory<UserAgentContext> {

    public static final String PROVIDER_ID = "default-user-agent-risk-factor-evaluator";

    @Override
    public RiskFactorEvaluator<UserAgentContext> create(KeycloakSession session) {
        return new DefaultUserAgentEvaluator(session);
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
