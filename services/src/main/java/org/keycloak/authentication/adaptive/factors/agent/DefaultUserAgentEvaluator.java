package org.keycloak.authentication.adaptive.factors.agent;

import org.keycloak.adaptive.RiskConfidence;
import org.keycloak.adaptive.factors.RiskFactorEvaluator;
import org.keycloak.adaptive.factors.UserContext;
import org.keycloak.models.KeycloakSession;

import java.util.Collection;

public class DefaultUserAgentEvaluator implements RiskFactorEvaluator<UserAgentContext> {
    private final KeycloakSession session;
    private final UserAgentContext userAgentFactor;

    public DefaultUserAgentEvaluator(KeycloakSession session) {
        this.session = session;
        this.userAgentFactor = (UserAgentContext) session.getProvider(UserContext.class, HeaderUserAgentContextFactory.PROVIDER_ID);
    }

    @Override
    public Double getRiskValue() {
        if (UserAgent.knownAgents.contains(userAgentFactor.getData())) {
            return RiskConfidence.VERY_CONFIDENT;
        } else {
            return RiskConfidence.SMALL;
        }
    }

    @Override
    public Collection<UserAgentContext> getUserContexts() {
        return null;
    }

    @Override
    public void evaluate() {

    }
}
