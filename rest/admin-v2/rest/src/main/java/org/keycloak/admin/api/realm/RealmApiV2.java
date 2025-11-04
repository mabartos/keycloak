package org.keycloak.admin.api.realm;

import jakarta.ws.rs.Path;
import org.keycloak.admin.api.AdminApi;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Objects;

public class RealmApiV2 implements AdminApi {
    private final KeycloakSession session;
    private final RealmModel realm;

    public RealmApiV2(KeycloakSession session) {
        this.session = session;
        this.realm = Objects.requireNonNull(session.getContext().getRealm());
    }

    @Path("clients")
    public ClientsApi clients() {
        return session.getProvider(ClientsApi.class);
    }

}
