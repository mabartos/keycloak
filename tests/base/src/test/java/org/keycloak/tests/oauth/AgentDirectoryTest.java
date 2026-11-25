package org.keycloak.tests.oauth;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedHashMap;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.agents.AgentAuthorization;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.PreAuthorizationRequestContext;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = AgentDirectoryTest.ServerConfig.class)
public class AgentDirectoryTest {
    private static final String RESOURCE = "https://tickets.example.test/mcp";
    private static final String ISSUER = "https://as.example.test";
    private static final List<String> SCOPES = List.of("openid", "profile", "email", "acr", "roles", "basic", "web-origins");

    @InjectAdminClient
    Keycloak admin;
    @InjectRealm(config = TestRealm.class)
    ManagedRealm realm;
    @InjectOAuthClient(config = Caller.class)
    OAuthClient oauth;
    @InjectKeycloakUrls
    KeycloakUrls urls;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests.oauth")
    RunOnServerClient runOnServer;

    @Test
    public void cimdConnectionsUseTheEntireDocumentUrl() {
        String registered = "https://clients.example.test/apps/support.json";
        String sibling = "https://clients.example.test/apps/other.json";
        try (AgentDirectoryAdmin api = new AgentDirectoryAdmin(admin, urls, realm)) {
            api.put("agents", Map.of("name", "Support", "clientId", registered, "cimd", true));
            api.put("agents", Map.of("name", "Other", "clientId", sibling, "cimd", true));
            api.put("servers", Map.of("name", "Tickets", "resource", RESOURCE));
            api.put("connections", Map.of("caller", registered, "resource", RESOURCE, "mode", "interactive", "scopes", SCOPES));
            String realmName = realm.getName();
            runOnServer.run(session -> {
                session.getContext().setRealm(session.realms().getRealmByName(realmName));
                MultivaluedHashMap<String, String> params = new MultivaluedHashMap<>();
                params.putSingle(OAuth2Constants.RESOURCE, RESOURCE);
                ClientPolicyException denied = Assertions.assertThrows(ClientPolicyException.class,
                        () -> AgentAuthorization.authorize(session, new PreAuthorizationRequestContext(sibling, params)));
                Assertions.assertEquals(OAuthErrorException.INVALID_TARGET, denied.getError());
                params.putSingle(OAuth2Constants.REDIRECT_URI, "http://127.0.0.1/callback");
                ClientPolicyException callback = Assertions.assertThrows(ClientPolicyException.class,
                        () -> AgentAuthorization.authorize(session, new PreAuthorizationRequestContext(registered, params)));
                Assertions.assertEquals(OAuthErrorException.INVALID_REQUEST, callback.getError());
            });
        }
    }

    @Test
    public void interactiveAccessAndRevocation() {
        try (AgentDirectoryAdmin api = new AgentDirectoryAdmin(admin, urls, realm)) {
            String agent = api.put("agents", Map.of("name", "Assistant", "clientId", "test-app"));
            Assertions.assertEquals("test-app", realm.admin().clients().get(agent).toRepresentation().getClientId());
            Assertions.assertEquals("true", realm.admin().clients().get(agent).toRepresentation().getAttributes().get(Constants.AGENT_CLIENT_ATTRIBUTE));
            Assertions.assertEquals(agent, api.list().get("agents").get(0).get("clientUuid").asText());
            api.put("servers", Map.of("name", "Tickets", "resource", RESOURCE, "clientId", "tickets"));
            Map<String, Object> link = Map.of("caller", "test-app", "resource", RESOURCE, "mode", "interactive", "scopes", SCOPES);
            String id = api.put("connections", link);
            Assertions.assertEquals(id, api.put("connections", link));
            Assertions.assertEquals(204, api.delete("agents", agent));
            Assertions.assertNull(realm.admin().clients().get(agent).toRepresentation().getAttributes().get(Constants.AGENT_CLIENT_ATTRIBUTE));
            RealmRepresentation update = new RealmRepresentation();
            update.setAttributes(Map.of("directory-test", "true"));
            realm.admin().update(update);
            Assertions.assertEquals(1, api.list().get("connections").size());

            AuthorizationEndpointResponse login = oauth.loginForm().resource(RESOURCE).doLogin("alice", "password");
            AccessTokenResponse tokens = oauth.accessTokenRequest(login.getCode()).resource(RESOURCE).send();
            Assertions.assertTrue(tokens.isSuccess(), tokens.getErrorDescription());
            Assertions.assertArrayEquals(new String[]{RESOURCE}, oauth.verifyToken(tokens.getAccessToken()).getAudience());
            Assertions.assertEquals(204, api.delete("connections", id));
            AccessTokenResponse refresh = oauth.refreshRequest(tokens.getRefreshToken()).send();
            Assertions.assertEquals("invalid_target", refresh.getError());
            Assertions.assertEquals(0, api.list().get("connections").size());
        }
    }

    @Test
    public void connectExistingRealmClients() {
        String resource = "urn:client:tickets";
        try (AgentDirectoryAdmin api = new AgentDirectoryAdmin(admin, urls, realm)) {
            String id = api.put("connections", Map.of("caller", "test-app", "resource", resource, "mode", "interactive", "scopes", SCOPES));
            Assertions.assertTrue(api.list().get("agents").isEmpty());
            Assertions.assertTrue(api.list().get("servers").isEmpty());

            AuthorizationEndpointResponse login = oauth.loginForm().resource(resource).doLogin("alice", "password");
            AccessTokenResponse tokens = oauth.accessTokenRequest(login.getCode()).resource(resource).send();
            Assertions.assertTrue(tokens.isSuccess(), tokens.getErrorDescription());
            Assertions.assertArrayEquals(new String[]{"tickets"}, oauth.verifyToken(tokens.getAccessToken()).getAudience());
            Assertions.assertEquals(204, api.delete("connections", id));
            Assertions.assertEquals(OAuthErrorException.INVALID_TARGET, oauth.refreshRequest(tokens.getRefreshToken()).send().getError());
        }
    }

    @Test
    public void enterpriseAssertionSelectsReceiverByAudience() {
        try (AgentDirectoryAdmin api = new AgentDirectoryAdmin(admin, urls, realm)) {
            api.put("agents", Map.of("name", "Assistant", "clientId", "test-app"));
            api.put("servers", Map.of("name", "Tickets", "resource", RESOURCE, "issuer", ISSUER));
            api.put("connections", Map.of("caller", "test-app", "resource", RESOURCE, "mode", "enterprise",
                    "downstreamClientId", "assistant-at-tickets", "scopes", SCOPES));
            AuthorizationEndpointResponse login = oauth.scope("openid").loginForm().doLogin("alice", "password");
            AccessTokenResponse tokens = oauth.accessTokenRequest(login.getCode()).send();
            Assertions.assertTrue(tokens.isSuccess(), tokens.getErrorDescription());
            AccessTokenResponse assertion = exchange(tokens.getIdToken(), ISSUER);
            Assertions.assertTrue(assertion.isSuccess(), assertion.getErrorDescription());
            JsonWebToken jwt = oauth.verifyToken(assertion.getAccessToken(), JsonWebToken.class);
            Assertions.assertArrayEquals(new String[]{ISSUER}, jwt.getAudience());
            Assertions.assertNull(jwt.getOtherClaims().get("resource"));
            Assertions.assertEquals("assistant-at-tickets", jwt.getOtherClaims().get("client_id"));
            Assertions.assertNull(assertion.getRefreshToken());
            Assertions.assertFalse(exchange(tokens.getIdToken(), ISSUER + "/other").isSuccess());
        }
    }

    private AccessTokenResponse exchange(String idToken, String issuer) {
        return oauth.tokenExchangeRequest(idToken, OAuth2Constants.ID_TOKEN_TYPE)
                .requestedTokenType(OAuth2Constants.IDENTITY_ASSERTION_JWT_TOKEN_TYPE).audience(issuer).send();
    }

    public static class Caller extends DefaultOAuthClientConfiguration {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return super.configure(client).fullScopeEnabled(true).attribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED, "true");
        }
    }

    public static class TestRealm implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.clients(ClientBuilder.create("tickets"), ClientBuilder.create(ISSUER))
                    .clientRoles(ISSUER, "access")
                    .clientRoles("tickets", "access")
                    .users(UserBuilder.create("alice").email("alice@example.test").firstName("Alice").lastName("Example")
                            .password("password").clientRoles(ISSUER, "access").clientRoles("tickets", "access"));
        }
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.AI_AGENTS, Profile.Feature.RESOURCE_INDICATORS, Profile.Feature.CIMD, Profile.Feature.IDENTITY_ASSERTION_JWT);
        }
    }
}
