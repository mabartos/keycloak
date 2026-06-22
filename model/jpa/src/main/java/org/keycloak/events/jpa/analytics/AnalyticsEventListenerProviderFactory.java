package org.keycloak.events.jpa.analytics;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class AnalyticsEventListenerProviderFactory implements EventListenerProviderFactory, EnvironmentDependentProviderFactory {

    public static final String ID = "analytics";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        JpaConnectionProvider connection = session.getProvider(JpaConnectionProvider.class);
        return new AnalyticsEventListenerProvider(connection.getEntityManager());
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
        return ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.ANALYTICS_DASHBOARDS);
    }
}
