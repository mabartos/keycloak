package org.keycloak.admin.api.client;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.resources.admin.RealmsAdminResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

public class DefaultClientsApiGroup implements ClientsApiGroup {
    private final KeycloakSession session;
    private final AdminAuth adminAuth;
    private final RealmsAdminResource realmsAdminResource;

    public DefaultClientsApiGroup(KeycloakSession session) {
        assertApiEnabled();
        this.session = session;
        this.adminAuth = AdminRoot.authenticateRealmAdminRequest(session);
        this.realmsAdminResource = new RealmsAdminResource(session, adminAuth, new TokenManager());
    }

    @Path("realms/{realmName}/clients")
    @Override
    public ClientsApi clients(@PathParam("realmName") String realmName) {
        var realm = session.realms().getRealmByName(realmName);
        if (realm == null) {
            throw new NotFoundException("Realm does not exist");
        }
        var adminPermissionEvaluator = AdminPermissions.evaluator(session, realm, adminAuth);
        return new DefaultClientsApi(session, adminPermissionEvaluator, realmsAdminResource.getRealmAdmin(realmName));
    }

    public static void assertApiEnabled() {
        if (!isApiEnabled()) {
            throw new NotFoundException();
        }
    }

    public static boolean isApiEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_ADMIN_API_V2);
    }

}
