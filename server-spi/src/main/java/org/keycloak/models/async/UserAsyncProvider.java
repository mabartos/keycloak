package org.keycloak.models.async;

import org.keycloak.provider.Provider;
import org.keycloak.storage.user.UserBulkUpdateProvider;
import org.keycloak.storage.user.async.UserLookupAsyncProvider;
import org.keycloak.storage.user.async.UserQueryAsyncProvider;
import org.keycloak.storage.user.async.UserRegistrationAsyncProvider;

public interface UserAsyncProvider extends Provider, UserLookupAsyncProvider, UserQueryAsyncProvider, UserRegistrationAsyncProvider, UserBulkUpdateProvider {
}
