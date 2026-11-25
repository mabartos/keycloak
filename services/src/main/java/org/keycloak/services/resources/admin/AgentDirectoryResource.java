package org.keycloak.services.resources.admin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oauth2.cimd.provider.AbstractPersistentClientIdMetadataDocumentProvider;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.agents.AgentDirectory;
import org.keycloak.protocol.oidc.agents.AgentDirectory.Agent;
import org.keycloak.protocol.oidc.agents.AgentDirectory.Connection;
import org.keycloak.protocol.oidc.agents.AgentDirectory.Directory;
import org.keycloak.protocol.oidc.agents.AgentDirectory.Mode;
import org.keycloak.protocol.oidc.agents.AgentDirectory.Server;
import org.keycloak.protocol.oidc.resourceindicators.ResourceIndicatorConstants;
import org.keycloak.protocol.oidc.resourceindicators.ResourceIndicatorValidation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.jboss.resteasy.reactive.NoCache;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AgentDirectoryResource {
    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder audit;
    private final RealmModel realm;
    private final AgentDirectory directory;

    AgentDirectoryResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder audit) {
        this.session = session;
        this.auth = auth;
        this.audit = audit.resource("AI_AGENTS");
        realm = session.getContext().getRealm();
        directory = new AgentDirectory(realm);
    }

    @GET
    @NoCache
    public Directory list() {
        auth.realm().requireViewRealm();
        return directory.list();
    }

    @PUT
    @Path("agents")
    public Agent saveAgent(Agent input) {
        auth.realm().requireManageRealm();
        require(input != null && text(input.clientId(), 1024), "Enter the client ID");
        ClientModel client = realm.getClientByClientId(input.clientId());
        Agent old = directory.agent(input.clientId());
        if (!input.cimd()) {
            requireOidcClient(client);
            require(!input.loopback(), "Loopback callback configuration applies to CIMD clients");
            require(client.getAttribute(AbstractPersistentClientIdMetadataDocumentProvider.CIMD_CACHE_EXPIRY_TIME_IN_SEC) == null,
                    "Register this client using its metadata document URL");
            auth.clients().requireConfigure(client);
            if (old != null) {
                require(!old.cimd() && old.isBoundTo(client), "Remove the old client binding first");
                return old;
            }
            boolean created = !"true".equals(client.getAttribute(Constants.AGENT_CLIENT_ATTRIBUTE));
            client.setAttribute(Constants.AGENT_CLIENT_ATTRIBUTE, "true");
            Agent saved = AgentDirectory.fromClient(client);
            audit(created, saved);
            return saved;
        }
        require(text(input.name(), 128), "Enter a name for the metadata document");
        https(input.clientId(), true);
        require(client == null || client.getAttribute(AbstractPersistentClientIdMetadataDocumentProvider.CIMD_CACHE_EXPIRY_TIME_IN_SEC) != null,
                "A locally registered client already uses this client ID");
        require(old == null || old.cimd() && (client == null || old.isBoundTo(client)), "Remove the old client binding first");
        String uuid = old != null ? old.clientUuid() : client == null ? null : client.getId();
        Agent saved = new Agent(input.name(), input.clientId(), true, input.loopback(), uuid, id(old == null ? null : old.id()));
        directory.save(saved);
        audit(old == null, saved);
        return saved;
    }

    @PUT
    @Path("servers")
    public Server saveServer(Server input) {
        auth.realm().requireManageRealm();
        require(input != null && text(input.name(), 128), "Enter an MCP server name");
        https(input.resource(), false);
        String uuid = null;
        if (input.issuer() != null) {
            require(Profile.isFeatureEnabled(Profile.Feature.IDENTITY_ASSERTION_JWT), "Enable identity-assertion-jwt for enterprise access");
            https(input.issuer(), false);
            require(input.clientId() == null, "Use a local resource client or an external authorization server");
            ClientModel authorizationServer = realm.getClientByClientId(input.issuer());
            requireOidcClient(authorizationServer);
            uuid = authorizationServer.getId();
        } else if (input.clientId() != null) {
            ClientModel client = realm.getClientByClientId(input.clientId());
            requireOidcClient(client);
            uuid = client.getId();
        }
        Server old = directory.server(input.resource());
        require(old == null || Objects.equals(old.clientUuid(), uuid) && Objects.equals(old.issuer(), input.issuer()),
                "Remove the old resource binding before changing its owner");
        Server saved = new Server(input.name(), input.resource(), input.clientId(), uuid, input.issuer(), scopes(input.supportedScopes()), id(old == null ? null : old.id()));
        directory.save(saved);
        audit(old == null, saved);
        return saved;
    }

    @PUT
    @Path("connections")
    public Connection saveConnection(Connection input) {
        auth.realm().requireManageRealm();
        require(input != null && input.mode() != null, "Choose a connection mode");
        Agent agent = directory.caller(input.caller());
        require(agent != null, "Choose a registered agent or an enabled OIDC client as the caller");
        Server server;
        if (AgentDirectory.isClientResource(input.resource())) {
            require(ResourceIndicatorValidation.isValidResourceIndicator(input.resource()), "The target client ID must form a valid client resource URN");
            ClientModel target = realm.getClientByClientId(input.resource().substring(ResourceIndicatorConstants.URN_CLIENT_PREFIX.length()));
            requireOidcClient(target);
            require(input.mode() != Mode.enterprise, "Select a registered resource for enterprise access");
            server = AgentDirectory.clientTarget(input.resource(), target.getId());
        } else {
            server = directory.server(input.resource());
            require(server != null, "Choose a registered MCP server or an enabled OIDC client as the destination");
        }
        require(server.issuer() == null || input.mode() == Mode.enterprise, "The connection mode must match the server's authorization server");
        require(server.issuer() != null ? text(input.downstreamClientId(), 255) : input.downstreamClientId() == null,
                "Specify a downstream client ID only for enterprise connections");
        if (input.mode() == Mode.delegated) {
            require(Profile.isFeatureEnabled(Profile.Feature.TOKEN_EXCHANGE_DELEGATION) && Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2) && realm.isAdminPermissionsEnabled(),
                    "Enable token-exchange-delegation and realm admin permissions for delegated connections");
        }
        if (input.mode() == Mode.enterprise) {
            require(Profile.isFeatureEnabled(Profile.Feature.IDENTITY_ASSERTION_JWT), "Enable identity-assertion-jwt for enterprise access");
        }
        if (input.mode() != Mode.interactive) {
            ClientModel client = realm.getClientByClientId(agent.clientId());
            require(agent.matches(client) && !client.isPublicClient(), "Select an enabled confidential caller");
            OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientModel(client);
            if (input.mode() == Mode.enterprise && server.issuer() == null) {
                require(config.getJWTAuthorizationGrantEnabled(), "Enable JWT Authorization Grant on the receiving client");
            } else {
                require(config.isStandardTokenExchangeEnabled(), "Enable standard token exchange on the caller");
            }
            if (server.issuer() != null) {
                boolean ambiguous = directory.serversForIssuer(server.issuer()).stream()
                        .anyMatch(other -> !other.id().equals(server.id()) && directory.find(agent, other, Mode.enterprise) != null);
                require(!ambiguous, "ID-JAG selects an authorization server by audience. This caller already has a connection to that issuer");
            }
            if (input.mode() == Mode.delegated) {
                require(client.isServiceAccountsEnabled() && server.clientUuid() != null && server.isBoundTo(realm.getClientById(server.clientUuid())),
                        "Delegation requires a caller service account and a registered target client");
            }
        }
        require(input.requiredRealmRole() == null || realm.getRole(input.requiredRealmRole()) != null, "Unknown required realm role");
        Connection old = directory.find(agent, server, input.mode());
        Connection saved = new Connection(agent.clientId(), server.resource(), input.mode(), scopes(input.scopes()),
                input.downstreamClientId(), input.requiredRealmRole(), agent.id(), server.id(), id(old == null ? null : old.id()));
        directory.save(saved);
        audit(old == null, saved);
        return saved;
    }

    @DELETE
    @Path("{collection}/{id}")
    public void remove(@PathParam("collection") String collection, @PathParam("id") String id) {
        auth.realm().requireManageRealm();
        if ("agents".equals(collection)) {
            ClientModel client = realm.getClientById(id);
            if (client != null) {
                auth.clients().requireConfigure(client);
            }
        }
        try {
            if (!directory.delete(collection, id)) {
                throw new NotFoundException();
            }
        } catch (IllegalStateException e) {
            throw ErrorResponse.error(e.getMessage(), Response.Status.CONFLICT);
        }
        audit.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
    }

    private void audit(boolean created, Object value) {
        audit.operation(created ? OperationType.CREATE : OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(value).success();
    }

    private static void requireOidcClient(ClientModel client) {
        require(client != null && client.isEnabled() && OIDCLoginProtocol.LOGIN_PROTOCOL.equals(client.getProtocol()), "Select an enabled OIDC client");
    }

    private static String id(String existing) {
        return existing == null ? UUID.randomUUID().toString() : existing;
    }

    private static List<String> scopes(List<String> values) {
        if (values == null) {
            return List.of();
        }
        require(values.size() <= 64 && values.stream().allMatch(s -> text(s, 128)
                && s.chars().allMatch(c -> c >= 0x21 && c <= 0x7e && c != 0x22 && c != 0x5c)), "Enter up to 64 valid OAuth scope names");
        return values.stream().distinct().sorted().toList();
    }

    private static boolean text(String value, int limit) {
        return value != null && !value.isBlank() && value.length() <= limit && value.equals(value.strip())
                && value.chars().noneMatch(Character::isISOControl);
    }

    private static void https(String value, boolean requirePath) {
        require(text(value, 1024), "Enter an HTTPS URL");
        try {
            URI uri = new URI(value);
            require("https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null && uri.getQuery() == null
                    && uri.getFragment() == null && uri.equals(uri.normalize()) && (!requirePath || uri.getPath().length() > 1),
                    "Enter an absolute HTTPS URL without credentials, query or fragment");
        } catch (URISyntaxException e) {
            throw ErrorResponse.error("Invalid HTTPS URL", Response.Status.BAD_REQUEST);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw ErrorResponse.error(message, Response.Status.BAD_REQUEST);
        }
    }
}
