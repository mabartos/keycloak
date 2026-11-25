package org.keycloak.protocol.oidc.agents;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutor;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutorFactory;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.agents.AgentDirectory.Agent;
import org.keycloak.protocol.oidc.agents.AgentDirectory.Connection;
import org.keycloak.protocol.oidc.agents.AgentDirectory.Mode;
import org.keycloak.protocol.oidc.agents.AgentDirectory.Server;
import org.keycloak.protocol.oidc.token.TokenInterceptorException;
import org.keycloak.protocol.oidc.token.TokenPostProcessorContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.PreAuthorizationRequestContext;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

public final class AgentAuthorization {
    public static final String DELEGATED_RESOURCE = "ai-agents.delegated-resource";
    public static final String SUBJECT_SCOPES = "ai-agents.subject-scopes";
    public static final String ASSERTED_RESOURCE = "ai-agents.asserted-resource";

    private static final String REFRESH_CONNECTION = "kc.agent.connection";

    private AgentAuthorization() {
    }

    public static boolean containsScopes(String granted, String requested) {
        return requested == null || requested.isBlank()
                || granted != null && Arrays.asList(granted.split(" +")).containsAll(Arrays.asList(requested.split(" +")));
    }

    public static void authorize(KeycloakSession session, PreAuthorizationRequestContext request) throws ClientPolicyException {
        RealmModel realm = session.getContext().getRealm();
        AgentDirectory directory = new AgentDirectory(realm);
        Agent agent = directory.agent(request.getClientId());
        String resource = request.getRequestParameters().getFirst(OAuth2Constants.RESOURCE);
        Server server = directory.connectionTarget(request.getClientId(), resource, Mode.interactive);
        if (server != null) {
            Agent caller = agent != null ? agent : directory.caller(request.getClientId());
            Connection link = caller == null ? null : directory.find(caller, server, Mode.interactive);
            if (link == null) {
                throw new ClientPolicyException(OAuthErrorException.INVALID_TARGET, "The caller has no interactive connection to this resource");
            }
            if (!link.allows(request.getRequestParameters().getFirst(OAuth2Constants.SCOPE))) {
                throw new ClientPolicyException(OAuthErrorException.INVALID_SCOPE, "The requested scopes exceed the connection");
            }
        }
        if (agent == null || !agent.cimd()) {
            return;
        }
        if (!Profile.isFeatureEnabled(Profile.Feature.CIMD) || server == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_TARGET, "A managed CIMD client must request a resource with an allowed connection");
        }
        String host = URI.create(agent.clientId()).getHost();
        ClientIdMetadataDocumentExecutor.Configuration config = new ClientIdMetadataDocumentExecutor.Configuration();
        List<String> hosts = agent.loopback() ? List.of(host, "localhost", "127.0.0.1", "[::1]") : List.of(host);
        String redirect = request.getRequestParameters().getFirst(OAuth2Constants.REDIRECT_URI);
        try {
            URI uri = URI.create(redirect == null ? "" : redirect);
            boolean loopback = agent.loopback() && List.of("localhost", "127.0.0.1", "[::1]").contains(Objects.toString(uri.getHost(), ""));
            if (uri.getHost() == null || !hosts.contains(uri.getHost()) || uri.getUserInfo() != null
                    || !("https".equals(uri.getScheme()) || loopback && "http".equals(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "The callback must use the registered host or an allowed loopback address");
        }
        config.setTrustedDomains(hosts);
        config.setRestrictSameDomain(!agent.loopback());
        config.setAcceptPublicClientWithConfidentialClientOnlyGrant(true);
        ClientIdMetadataDocumentExecutorFactory factory = (ClientIdMetadataDocumentExecutorFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(ClientPolicyExecutorProvider.class, ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID);
        ClientModel existing = realm.getClientByClientId(agent.clientId());
        if (existing != null && !agent.matches(existing)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT, "The registered CIMD client binding has changed");
        }
        factory.create(session).executeManagedRequest(config, request);
        ClientModel client = realm.getClientByClientId(agent.clientId());
        if (existing == null && client != null && agent.clientUuid() == null) {
            directory.bindNewCimdClient(agent, client);
        }
        if (!agent.matches(client)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT, "The client is not registered through CIMD");
        }
        if (client.isPublicClient()) {
            OIDCAdvancedConfigWrapper configWrapper = OIDCAdvancedConfigWrapper.fromClientModel(client);
            if (!OAuth2Constants.PKCE_METHOD_S256.equals(configWrapper.getPkceCodeChallengeMethod())) {
                configWrapper.setPkceCodeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
            }
        }
    }

    public static void authorizeAssertion(RealmModel realm, ClientModel caller, UserModel user, AccessToken subject,
                                          String issuer, String requestedScope, AccessToken evaluated) throws ClientPolicyException {
        AgentDirectory directory = new AgentDirectory(realm);
        List<Server> targets = directory.serversForIssuer(issuer);
        if (directory.agent(caller.getClientId()) == null && targets.isEmpty()) {
            return;
        }
        ClientModel authority = realm.getClientByClientId(issuer);
        List<Connection> links = targets.stream()
                .filter(target -> authority != null && authority.isEnabled() && authority.getId().equals(target.clientUuid()))
                .map(target -> directory.find(caller, target, Mode.enterprise))
                .filter(Objects::nonNull).toList();
        if (links.size() != 1 || subject == null || !caller.getClientId().equals(subject.getIssuedFor()) || !subject.hasAudience(caller.getClientId())
                || !links.get(0).permitsUser(realm, user) || !evaluated.hasAudience(issuer)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_TARGET, "No eligible enterprise connection for this caller and authorization server");
        }
        Connection link = links.get(0);
        if (!link.allows(requestedScope) || !link.allows(evaluated.getScope())) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_SCOPE, "The requested scopes exceed the enterprise connection");
        }
        evaluated.audience(issuer);
        evaluated.setOtherClaims(OAuth2Constants.CLIENT_ID, link.downstreamClientId());
    }

    public static boolean authorizeAssertionRedemption(RealmModel realm, ClientModel caller, UserModel user, JsonWebToken assertion,
                                                      String resource, String requestedScope) {
        AgentDirectory directory = new AgentDirectory(realm);
        Server server = directory.server(resource);
        if (server == null && directory.agent(caller.getClientId()) == null) {
            return false;
        }
        Object bound = assertion.getOtherClaims().get(OAuth2Constants.RESOURCE);
        Connection link = directory.find(caller, server, Mode.enterprise);
        if (link == null || server.issuer() != null || bound != null && !bound.equals(resource) || !link.permitsUser(realm, user)) {
            throw new IllegalArgumentException("Supply a registered resource with an eligible enterprise connection");
        }
        Object scopes = assertion.getOtherClaims().get(OAuth2Constants.SCOPE);
        if (scopes != null && !(scopes instanceof String) || !link.allows(requestedScope) || !containsScopes((String) scopes, requestedScope)) {
            throw new IllegalArgumentException("The requested scopes exceed the assertion or enterprise connection");
        }
        return true;
    }

    public static boolean bindResource(KeycloakSession session, TokenPostProcessorContext context, String resource, String grantType) {
        RealmModel realm = session.getContext().getRealm();
        AgentDirectory directory = new AgentDirectory(realm);
        ClientModel client = context.clientSessionCtx().getClientSession().getClient();
        Mode mode = null;
        if (OAuth2Constants.AUTHORIZATION_CODE.equals(grantType) || OAuth2Constants.REFRESH_TOKEN.equals(grantType)) {
            mode = Mode.interactive;
        } else if (resource != null && resource.equals(context.clientSessionCtx().getAttribute(DELEGATED_RESOURCE, String.class))) {
            mode = Mode.delegated;
        } else if (resource != null && resource.equals(context.clientSessionCtx().getAttribute(ASSERTED_RESOURCE, String.class))) {
            mode = Mode.enterprise;
        }
        Server server = directory.connectionTarget(client.getClientId(), resource, mode);
        Object refreshConnection = context.requestRefreshToken() == null ? null : context.requestRefreshToken().getOtherClaims().get(REFRESH_CONNECTION);
        if (server == null) {
            if (refreshConnection != null) {
                throw invalidTarget();
            }
            String[] audiences = context.accessToken().getAudience();
            if (resource == null && audiences != null && Arrays.stream(audiences).anyMatch(aud -> directory.server(aud) != null)) {
                throw invalidTarget();
            }
            return false;
        }
        if (mode == null) {
            throw invalidTarget();
        }
        Connection link = directory.find(client, server, mode);
        if (link == null || refreshConnection != null && !refreshConnection.equals(link.id()) || server.clientUuid() != null && !server.isBoundTo(realm.getClientById(server.clientUuid()))
                || !link.permitsUser(realm, context.clientSessionCtx().getClientSession().getUserSession().getUser())) {
            throw invalidTarget();
        }
        if (mode == Mode.delegated && !context.accessToken().hasAudience(server.clientId())) {
            throw invalidTarget();
        }
        String scope = context.accessToken().getScope();
        if (!link.allows(scope) || mode != Mode.interactive && !containsScopes(context.clientSessionCtx().getAttribute(SUBJECT_SCOPES, String.class), scope)) {
            throw new TokenInterceptorException(OAuthErrorException.INVALID_SCOPE, "Issued scopes exceed the agent connection");
        }
        if (context.refreshToken() != null) {
            context.refreshToken().setOtherClaims(REFRESH_CONNECTION, link.id());
        }
        return true;
    }

    private static TokenInterceptorException invalidTarget() {
        return new TokenInterceptorException(OAuthErrorException.INVALID_TARGET, "No eligible agent connection for this resource and grant");
    }
}
