package org.keycloak.models;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public interface NotStoredClientModel extends ClientModel {
    @Override
    default void updateClient() {
        throw new UnsupportedOperationException();
    }

    @Override
    default RoleModel addRole(String name) {
        return null;
    }

    @Override
    default RoleModel addRole(String id, String name) {
        return null;
    }

    @Override
    default boolean removeRole(RoleModel role) {
        return false;
    }

    @Override
    default Stream<RoleModel> getRolesStream(Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    default Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        return Stream.empty();
    }

    @Override
    default void setClientId(String clientId) {

    }

    @Override
    default void setName(String name) {

    }

    @Override
    default void setDescription(String description) {
    }

    @Override
    default void setEnabled(boolean enabled) {

    }

    @Override
    default void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole) {

    }

    @Override
    default void setSurrogateAuthRequired(boolean surrogateAuthRequired) {

    }

    @Override
    default void setWebOrigins(Set<String> webOrigins) {

    }

    @Override
    default void addWebOrigin(String webOrigin) {

    }

    @Override
    default void removeWebOrigin(String webOrigin) {

    }

    @Override
    default void setRedirectUris(Set<String> redirectUris) {

    }

    @Override
    default void addRedirectUri(String redirectUri) {

    }

    @Override
    default void removeRedirectUri(String redirectUri) {

    }

    @Override
    default void setManagementUrl(String url) {

    }


    @Override
    default void setRootUrl(String url) {

    }


    @Override
    default void setBaseUrl(String url) {

    }


    @Override
    default void setBearerOnly(boolean only) {

    }


    @Override
    default void setNodeReRegistrationTimeout(int timeout) {

    }


    @Override
    default void setClientAuthenticatorType(String clientAuthenticatorType) {

    }

    @Override
    default boolean validateSecret(String secret) {
        return false;
    }


    @Override
    default void setSecret(String secret) {

    }


    @Override
    default void setRegistrationToken(String registrationToken) {

    }


    @Override
    default void setProtocol(String protocol) {

    }

    @Override
    default void setAttribute(String name, String value) {

    }

    @Override
    default void removeAttribute(String name) {

    }


    @Override
    default void removeAuthenticationFlowBindingOverride(String binding) {

    }

    @Override
    default void setAuthenticationFlowBindingOverride(String binding, String flowId) {

    }


    @Override
    default void setFrontchannelLogout(boolean flag) {

    }


    @Override
    default void setFullScopeAllowed(boolean value) {

    }

    @Override
    default void setConsentRequired(boolean consentRequired) {

    }


    @Override
    default void setStandardFlowEnabled(boolean standardFlowEnabled) {

    }


    @Override
    default void setImplicitFlowEnabled(boolean implicitFlowEnabled) {

    }


    @Override
    default void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {

    }


    @Override
    default void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {

    }

    @Override
    default RealmModel getRealm() {
        return null;
    }

    @Override
    default void addClientScope(ClientScopeModel clientScope, boolean defaultScope) {

    }

    @Override
    default void addClientScopes(Set<ClientScopeModel> clientScopes, boolean defaultScope) {

    }

    @Override
    default void removeClientScope(ClientScopeModel clientScope) {

    }


    @Override
    default void setNotBefore(int notBefore) {

    }


    @Override
    default void registerNode(String nodeHost, int registrationTime) {

    }

    @Override
    default void unregisterNode(String nodeHost) {

    }


    @Override
    default ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        return null;
    }

    @Override
    default void removeProtocolMapper(ProtocolMapperModel mapping) {

    }

    @Override
    default void updateProtocolMapper(ProtocolMapperModel mapping) {

    }

    @Override
    default ProtocolMapperModel getProtocolMapperById(String id) {
        return null;
    }

    @Override
    default ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        return null;
    }

    @Override
    default Stream<RoleModel> getScopeMappingsStream() {
        return Stream.empty();
    }

    @Override
    default Stream<RoleModel> getRealmScopeMappingsStream() {
        return Stream.empty();
    }

    @Override
    default void addScopeMapping(RoleModel role) {

    }

    @Override
    default void deleteScopeMapping(RoleModel role) {

    }

    @Override
    default boolean hasScope(RoleModel role) {
        return false;
    }
}
