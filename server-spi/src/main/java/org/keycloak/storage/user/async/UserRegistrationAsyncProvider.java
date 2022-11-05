package org.keycloak.storage.user.async;

import io.smallrye.mutiny.Uni;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public interface UserRegistrationAsyncProvider {

    /**
     * All storage providers that implement this interface will be looped through.
     * If this method returns null, then the next storage provider's addUser() method will be called.
     * If no storage providers handle the add, then the user will be created in local storage.
     * <p>
     * Returning null is useful when you want optional support for adding users.  For example,
     * our LDAP provider can enable and disable the ability to add users.
     *
     * @param realm    a reference to the realm
     * @param username a username the created user will be assigned
     * @return a model of created user
     */
    Uni<UserModel> addUser(RealmModel realm, String username);

    /**
     * Called if user originated from this provider.
     * <p>
     * <p>
     * If a local user is linked to this provider, this method will be called before
     * local storage's removeUser() method is invoked.
     * <p>
     * If you are using an import strategy, and this is a local user linked to this provider,
     * this method will be called before local storage's removeUser() method is invoked.  Also,
     * you DO NOT need to remove the imported user.  The runtime will handle this for you.
     *
     * @param realm a reference to the realm
     * @param user  a reference to the user that is removed
     * @return true if the user was removed, false otherwise
     */
    Uni<Boolean> removeUser(RealmModel realm, UserModel user);

}
