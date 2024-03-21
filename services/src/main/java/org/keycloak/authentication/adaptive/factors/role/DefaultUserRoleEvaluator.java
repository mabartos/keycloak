package org.keycloak.authentication.adaptive.factors.role;

import org.keycloak.adaptive.factors.RiskFactorEvaluator;
import org.keycloak.models.KeycloakSession;

import java.util.Collection;

public class DefaultUserRoleEvaluator implements RiskFactorEvaluator<UserRoleContext> {
    private final KeycloakSession session;

    public DefaultUserRoleEvaluator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Double getRiskValue() {
        return null;
    }

    @Override
    public Collection<UserRoleContext> getUserContexts() {
        return null;
    }

    @Override
    public void evaluate() {

    }
}
