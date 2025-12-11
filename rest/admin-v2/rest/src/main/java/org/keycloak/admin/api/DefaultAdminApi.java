package org.keycloak.admin.api;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Path;

import org.keycloak.Config;
import org.keycloak.admin.api.client.ClientsApiGroup;
import org.keycloak.admin.api.client.DefaultClientsApiGroup;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.services.resources.admin.RealmsAdminResource;

public class DefaultAdminApi implements AdminApi {
    private final KeycloakSession session;
    private final RealmsAdminResource realmsAdminResource;
    private final RealmAdminResource realmAdminResource;
    private final AdminAuth auth;

    public DefaultAdminApi(KeycloakSession session, String realmName) {
        this.session = session;
        this.auth = AdminRoot.authenticateRealmAdminRequest(session);

        // TODO: refine permissions
        if (!auth.getRealm().getName().equals(Config.getAdminRealm()) || !auth.hasRealmRole(AdminRoles.ADMIN)) {
            throw new NotAuthorizedException("Wrong permissions");
        }
        this.realmsAdminResource = new RealmsAdminResource(session, auth, new TokenManager());
        this.realmAdminResource = realmsAdminResource.getRealmAdmin(realmName);
    }

    @Path("clients")
    @Override
    public ClientsApiGroup clientsGroupDefault() {
        return clientsGroupV2();
    }

    @Path("clients/v2")
    @Override
    public ClientsApiGroup clientsGroupV2() {
        return new DefaultClientsApiGroup(session, realmAdminResource);
    }
}
