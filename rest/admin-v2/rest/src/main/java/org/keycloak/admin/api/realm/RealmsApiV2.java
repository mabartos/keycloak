package org.keycloak.admin.api.realm;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.keycloak.Config;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminRoot;

import java.util.Optional;

public class RealmsApiV2 implements RealmsApi {
    private final KeycloakSession session;
    private final AdminAuth auth;

    public RealmsApiV2(KeycloakSession session) {
        this.session = session;
        this.auth = AdminRoot.authenticateRealmAdminRequest(session);

        // TODO: refine permissions
        if (!auth.getRealm().getName().equals(Config.getAdminRealm()) || !auth.hasRealmRole(AdminRoles.ADMIN)) {
            throw new NotAuthorizedException("Wrong permissions");
        }
    }

    @Path("{name}")
    public RealmApiV2 realm(@PathParam("name") String name) {
        var realm = Optional.ofNullable(session.realms().getRealmByName(name)).orElseThrow(() -> new NotFoundException("Realm cannot be found"));
        session.getContext().setRealm(realm);
        return new RealmApiV2(session);
    }
}
