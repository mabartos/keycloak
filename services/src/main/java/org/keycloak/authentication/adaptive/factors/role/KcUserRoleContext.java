package org.keycloak.authentication.adaptive.factors.role;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class KcUserRoleContext implements UserRoleContext {
    private final KeycloakSession session;
    private Set<RoleModel> data;
    private boolean isInitialized;

    public KcUserRoleContext(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean isDataInitialized() {
        return isInitialized;
    }

    @Override
    public void initData() {
        this.data = Optional.ofNullable(session.getContext())
                .map(KeycloakContext::getAuthenticationSession)
                .map(AuthenticationSessionModel::getAuthenticatedUser)
                .map(RoleMapperModel::getRoleMappingsStream)
                .map(f -> f.collect(Collectors.toSet()))
                .orElseGet(Collections::emptySet);
        this.isInitialized = true;
    }

    @Override
    public Set<RoleModel> getData() {
        return data;
    }
}
