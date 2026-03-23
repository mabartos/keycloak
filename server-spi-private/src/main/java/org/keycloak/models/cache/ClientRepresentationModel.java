package org.keycloak.models.cache;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.NotStoredClientModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class ClientRepresentationModel implements NotStoredClientModel {
    private final ClientRepresentation representation;

    public ClientRepresentationModel(ClientRepresentation representation) {
        this.representation = representation;
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public String getClientId() {
        return "";
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isAlwaysDisplayInConsole() {
        return false;
    }

    @Override
    public boolean isSurrogateAuthRequired() {
        return false;
    }

    @Override
    public Set<String> getWebOrigins() {
        return Set.of();
    }

    @Override
    public Set<String> getRedirectUris() {
        return new HashSet<>(representation.getRedirectUris());
    }

    @Override
    public String getManagementUrl() {
        return "";
    }

    @Override
    public String getRootUrl() {
        return "";
    }

    @Override
    public String getBaseUrl() {
        return "";
    }

    @Override
    public boolean isBearerOnly() {
        return false;
    }

    @Override
    public int getNodeReRegistrationTimeout() {
        return 0;
    }

    @Override
    public String getClientAuthenticatorType() {
        return "";
    }

    @Override
    public String getSecret() {
        return "";
    }

    @Override
    public String getRegistrationToken() {
        return "";
    }

    @Override
    public String getProtocol() {
        return "";
    }

    @Override
    public String getAttribute(String name) {
        return "";
    }

    @Override
    public Map<String, String> getAttributes() {
        return Map.of();
    }

    @Override
    public String getAuthenticationFlowBindingOverride(String binding) {
        return getAuthenticationFlowBindingOverrides().get(binding);
    }

    @Override
    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        return representation.getAuthenticationFlowBindingOverrides();
    }

    @Override
    public boolean isFrontchannelLogout() {
        return false;
    }

    @Override
    public boolean isFullScopeAllowed() {
        return false;
    }

    @Override
    public boolean isPublicClient() {
        return false;
    }

    @Override
    public void setPublicClient(boolean flag) {

    }

    @Override
    public boolean isConsentRequired() {
        return false;
    }

    @Override
    public boolean isStandardFlowEnabled() {
        return false;
    }

    @Override
    public boolean isImplicitFlowEnabled() {
        return false;
    }

    @Override
    public boolean isDirectAccessGrantsEnabled() {
        return false;
    }

    @Override
    public boolean isServiceAccountsEnabled() {
        return false;
    }

    @Override
    public Map<String, ClientScopeModel> getClientScopes(boolean defaultScope) {
        return Map.of();
    }

    @Override
    public int getNotBefore() {
        return 0;
    }

    @Override
    public Map<String, Integer> getRegisteredNodes() {
        return Map.of();
    }

    @Override
    public Stream<ProtocolMapperModel> getProtocolMappersStream() {
        return Stream.empty();
    }
}
