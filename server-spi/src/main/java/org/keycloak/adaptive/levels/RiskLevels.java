package org.keycloak.adaptive.levels;

import org.keycloak.provider.Provider;

import java.util.Set;

public interface RiskLevels extends Provider {

    Set<RiskLevel> getRiskLevels();
}
