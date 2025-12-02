package org.keycloak.models.mapper;

import java.util.Optional;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.services.resources.admin.RealmAdminResource;

public class MapStructClientModelContext implements ModelMapperContext {
    private final KeycloakSession session;
    private final RealmAdminResource realmAdminResource;
    private RealmModel realmModel;
    private ClientResource clientResource;

    public MapStructClientModelContext(KeycloakSession session, RealmModel realmModel, RealmAdminResource realmAdminResource, ClientResource clientResource) {
        this.session = session;
        this.realmModel = realmModel;
        this.realmAdminResource = realmAdminResource;
        this.clientResource = clientResource;
    }

    public KeycloakSession getSession() {
        return session;
    }

    public RealmAdminResource getRealmAdminResource() {
        return realmAdminResource;
    }

    public Optional<ClientResource> getClientResource() {
        return Optional.ofNullable(clientResource);
    }

    public Optional<RealmModel> getRealm() {
        return Optional.ofNullable(realmModel);
    }

    public MapStructClientModelContext setClientResource(ClientResource clientResource) {
        this.clientResource = clientResource;
        return this;
    }

    public MapStructClientModelContext setRealm(RealmModel realmModel) {
        this.realmModel = realmModel;
        return this;
    }
}
