package org.keycloak.authentication.adaptive.levels;

import org.keycloak.adaptive.levels.RiskLevel;
import org.keycloak.adaptive.levels.RiskLevels;
import org.keycloak.adaptive.levels.SimpleRiskLevel;

import java.util.Set;

public class SimpleRiskLevels implements RiskLevels {
    static final RiskLevel LOW = new SimpleRiskLevel("LOW", 0.0, 0.3);
    static final RiskLevel MEDIUM = new SimpleRiskLevel("MEDIUM", 0.31, 0.75);
    static final RiskLevel HIGH = new SimpleRiskLevel("HIGH", 0.76, 1.0);

    @Override
    public Set<RiskLevel> getRiskLevels() {
        return Set.of(LOW, MEDIUM, HIGH);
    }

    @Override
    public void close() {

    }
}
