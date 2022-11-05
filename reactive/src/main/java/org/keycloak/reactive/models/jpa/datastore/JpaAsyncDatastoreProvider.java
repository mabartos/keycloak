package org.keycloak.reactive.models.jpa.datastore;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.async.DatastoreAsyncProvider;
import org.keycloak.models.async.UserAsyncProvider;

public class JpaAsyncDatastoreProvider implements DatastoreAsyncProvider {

    private final KeycloakSession session;

    public JpaAsyncDatastoreProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public UserAsyncProvider users() {
        return session.getProvider(UserAsyncProvider.class);
    }

    @Override
    public void close() {

    }
}
