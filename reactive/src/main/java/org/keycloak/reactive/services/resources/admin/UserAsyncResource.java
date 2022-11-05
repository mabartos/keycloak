package org.keycloak.reactive.services.resources.admin;

import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

public class UserAsyncResource {
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final UserModel user;
    private final UserResource userResource;

    @Inject
    protected KeycloakSession session;

    public UserAsyncResource(RealmModel realm, UserModel user, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.user = user;
        this.adminEvent = adminEvent.resource(ResourceType.USER);
        this.userResource = new UserResource(realm, user, auth, adminEvent);
    }

    /**
     * Get representation of the user
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<UserRepresentation> getUser() {
        return Uni.createFrom().item(userResource::getUser);
    }
}
