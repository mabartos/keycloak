package org.keycloak.models.async;

import org.keycloak.provider.Provider;

public interface DatastoreAsyncProvider extends Provider {
    UserAsyncProvider users();
}
