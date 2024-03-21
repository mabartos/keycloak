package org.keycloak.adaptive.manager;

import org.keycloak.adaptive.engines.RiskEngine;
import org.keycloak.adaptive.levels.RiskLevelManager;
import org.keycloak.provider.Provider;

public interface AdaptiveAuthnManager extends Provider {

    RiskEngine getRiskEngine();

    RiskLevelManager getRiskLevelManager();

}
