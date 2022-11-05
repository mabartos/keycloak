package org.keycloak.reactive.models.jpa.datastore;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.async.DatastoreAsyncProvider;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.storage.async.DatastoreAsyncProviderFactory;

public class JpaAsyncDatastoreProviderFactory implements DatastoreAsyncProviderFactory, EnvironmentDependentProviderFactory {

    private static final String PROVIDER_ID = "jpa-async";

    @Override
    public DatastoreAsyncProvider create(KeycloakSession session) {
        return new JpaAsyncDatastoreProvider(session);
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

    @Override
    public boolean isSupported() {
        // TODO feature
        return true;
    }
}
