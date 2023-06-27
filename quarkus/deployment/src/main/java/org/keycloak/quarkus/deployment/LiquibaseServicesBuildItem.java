package org.keycloak.quarkus.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

import java.util.List;
import java.util.Map;

public final class LiquibaseServicesBuildItem extends SimpleBuildItem {

    private final Map<String, List<String>> services;

    public LiquibaseServicesBuildItem(Map<String, List<String>> services) {
        this.services = services;
    }

    public Map<String, List<String>> getServices() {
        return services;
    }

}
