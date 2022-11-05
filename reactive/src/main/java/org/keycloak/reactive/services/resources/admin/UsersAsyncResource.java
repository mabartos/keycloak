package org.keycloak.reactive.services.resources.admin;

import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.time.Duration;

@Path("/some")
public class UsersAsyncResource {

    @Inject
    KeycloakSession session;

    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public UsersAsyncResource(RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.adminEvent = adminEvent.resource(ResourceType.USER);
    }

    /**
     * Get representation of the user
     *
     * @param id User id
     * @return
     */
    @Path("{id}")
    public Uni<UserAsyncResource> user(final @PathParam("id") String id) {
        return session.asyncStore()
                .users()
                .getUserById(realm, id)
                .onItem()
                .transform(f -> {
                    final UserAsyncResource resource = new UserAsyncResource(realm, f, auth, adminEvent);
                    ResteasyProviderFactory.getInstance().injectProperties(resource);
                    return resource;
                })
                .ifNoItem()
                .after(Duration.ofMillis(300)) // ?? Not so goooood
                .failWith(() -> auth.users().canQuery() ? new NotFoundException("User not found") : new ForbiddenException());
    }
}
