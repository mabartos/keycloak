package org.keycloak.adaptive.levels;

public class SimpleRiskLevel implements RiskLevel {
    private final String name;
    private double lowestRiskValue;
    private double highestRiskValue;

    public SimpleRiskLevel(String name, double lowestRiskValue, double highestRiskValue) {
        this.name = name;
        this.lowestRiskValue = lowestRiskValue;
        this.highestRiskValue = highestRiskValue;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getLowestRiskValue() {
        return lowestRiskValue;
    }

    @Override
    public void setLowestRiskValue(double value) {
        this.lowestRiskValue = value;
    }

    @Override
    public double getHighestRiskValue() {
        return highestRiskValue;
    }

    @Override
    public void setHighestRiskValue(double value) {
        this.highestRiskValue = value;
    }

    @Override
    public boolean matchesRisk(double riskValue) {
        return riskValue >= getLowestRiskValue() && riskValue <= getHighestRiskValue();
    }
}
