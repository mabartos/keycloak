package org.keycloak.protocol.oidc.agents;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oauth2.cimd.provider.AbstractPersistentClientIdMetadataDocumentProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.resourceindicators.ResourceIndicatorConstants;
import org.keycloak.util.JsonSerialization;

public final class AgentDirectory {
    public enum Mode { interactive, delegated, enterprise }

    public record Agent(String name, String clientId, boolean cimd, boolean loopback, String clientUuid, String id) {
        public boolean matches(ClientModel client) {
            return isBoundTo(client) && client.isEnabled() && (!cimd || Profile.isFeatureEnabled(Profile.Feature.CIMD));
        }

        public boolean isBoundTo(ClientModel client) {
            if (client == null || !client.getClientId().equals(clientId)) {
                return false;
            }
            String binding = cimd && clientUuid == null ? client.getRealm().getAttribute(CIMD_BINDING + id) : clientUuid;
            return client.getId().equals(binding) && (!cimd || client.getAttribute(AbstractPersistentClientIdMetadataDocumentProvider.CIMD_CACHE_EXPIRY_TIME_IN_SEC) != null);
        }
    }

    public record Server(String name, String resource, String clientId, String clientUuid, String issuer, List<String> supportedScopes, String id) {
        public boolean isBoundTo(ClientModel client) {
            return client != null && client.isEnabled() && client.getId().equals(clientUuid) && client.getClientId().equals(clientId);
        }
    }

    public record Connection(String caller, String resource, Mode mode, List<String> scopes, String downstreamClientId, String requiredRealmRole,
                             String agentId, String serverId, String id) {
        public boolean permitsUser(RealmModel realm, UserModel user) {
            if (requiredRealmRole == null) {
                return true;
            }
            RoleModel role = realm.getRole(requiredRealmRole);
            return role != null && user != null && user.hasRole(role);
        }

        public boolean allows(String scope) {
            return scope == null || scope.isBlank() || scopes.containsAll(Arrays.asList(scope.split(" +")));
        }
    }

    public record Directory(List<Agent> agents, List<Server> servers, List<Connection> connections) {}

    private static final String AGENT = Constants.AGENT_DIRECTORY_ATTRIBUTE_PREFIX + "agent.";
    private static final String CIMD_BINDING = Constants.AGENT_DIRECTORY_ATTRIBUTE_PREFIX + "cimd-binding.";
    private static final String SERVER = Constants.AGENT_DIRECTORY_ATTRIBUTE_PREFIX + "server.";
    private static final String CONNECTION = Constants.AGENT_DIRECTORY_ATTRIBUTE_PREFIX + "connection.";

    private final RealmModel realm;

    public AgentDirectory(RealmModel realm) {
        this.realm = realm;
    }

    public Directory list() {
        Set<String> clientIds = new HashSet<>();
        Stream<Agent> local = realm.searchClientByAttributes(Map.of(Constants.AGENT_CLIENT_ATTRIBUTE, "true"), null, null)
                .filter(client -> OIDCLoginProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())
                        && client.getAttribute(AbstractPersistentClientIdMetadataDocumentProvider.CIMD_CACHE_EXPIRY_TIME_IN_SEC) == null)
                .map(AgentDirectory::fromClient);
        List<Agent> agents = Stream.concat(entries(AGENT, Agent.class).map(this::withClientBinding), local)
                .filter(agent -> clientIds.add(agent.clientId())).toList();
        return new Directory(agents, entries(SERVER, Server.class).toList(),
                entries(CONNECTION, Connection.class).toList());
    }

    public static Agent fromClient(ClientModel client) {
        String name = client.getName();
        return new Agent(name == null || name.isBlank() ? client.getClientId() : name, client.getClientId(), false, false, client.getId(), client.getId());
    }

    private Agent withClientBinding(Agent agent) {
        if (agent.cimd() && agent.clientUuid() == null) {
            String uuid = realm.getAttribute(CIMD_BINDING + agent.id());
            if (uuid != null && agent.isBoundTo(realm.getClientById(uuid))) {
                return new Agent(agent.name(), agent.clientId(), true, agent.loopback(), uuid, agent.id());
            }
        }
        return agent;
    }

    public List<Server> serversForIssuer(String issuer) {
        return entries(SERVER, Server.class).filter(server -> issuer.equals(server.issuer())).toList();
    }

    public Agent agent(String clientId) {
        return read(AGENT, clientId, Agent.class);
    }

    public Agent caller(String clientId) {
        Agent registered = agent(clientId);
        if (registered != null || clientId == null) {
            return registered;
        }
        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null || !client.isEnabled() || !OIDCLoginProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())
                || client.getAttribute(AbstractPersistentClientIdMetadataDocumentProvider.CIMD_CACHE_EXPIRY_TIME_IN_SEC) != null) {
            return null;
        }
        return fromClient(client);
    }

    public Server server(String resource) {
        return read(SERVER, resource, Server.class);
    }

    public static boolean isClientResource(String resource) {
        return resource != null && resource.startsWith(ResourceIndicatorConstants.URN_CLIENT_PREFIX);
    }

    public static Server clientTarget(String resource, String uuid) {
        String clientId = resource.substring(ResourceIndicatorConstants.URN_CLIENT_PREFIX.length());
        return new Server(clientId, resource, clientId, uuid, null, List.of(), uuid);
    }

    public Server connectionTarget(String caller, String resource, Mode mode) {
        if (!isClientResource(resource)) {
            return server(resource);
        }
        Connection link = caller == null || mode == null ? null : read(CONNECTION, connectionKey(caller, resource, mode), Connection.class);
        return link == null ? null : clientTarget(resource, link.serverId());
    }

    public Connection find(ClientModel caller, Server server, Mode mode) {
        if (server == null || caller == null) {
            return null;
        }
        Agent agent = caller(caller.getClientId());
        if (agent == null || !agent.matches(caller)) {
            return null;
        }
        return find(agent, server, mode);
    }

    public Connection find(Agent agent, Server server, Mode mode) {
        Connection link = read(CONNECTION, connectionKey(agent.clientId(), server.resource(), mode), Connection.class);
        return link != null && server.id().equals(link.serverId())
                && (agent.id().equals(link.agentId()) || !agent.cimd() && agent.clientUuid().equals(link.agentId())) ? link : null;
    }

    public void bindNewCimdClient(Agent agent, ClientModel client) {
        realm.setAttribute(CIMD_BINDING + agent.id(), client.getId());
    }

    public void save(Agent agent) {
        write(AGENT, agent.clientId(), agent);
    }

    public void save(Server server) {
        write(SERVER, server.resource(), server);
    }

    public void save(Connection connection) {
        write(CONNECTION, connectionKey(connection.caller(), connection.resource(), connection.mode()), connection);
    }

    public boolean delete(String collection, String id) {
        switch (collection) {
            case "connections" -> {
                for (Connection link : entries(CONNECTION, Connection.class).toList()) {
                    if (id.equals(link.id())) {
                        remove(CONNECTION, connectionKey(link.caller(), link.resource(), link.mode()));
                        return true;
                    }
                }
            }
            case "agents" -> {
                for (Agent agent : entries(AGENT, Agent.class).toList()) {
                    if (id.equals(agent.id())) {
                        ensureUnlinked(id);
                        remove(AGENT, agent.clientId());
                        if (agent.cimd()) {
                            realm.removeAttribute(CIMD_BINDING + id);
                        }
                        return true;
                    }
                }
                ClientModel client = realm.getClientById(id);
                if (client != null && "true".equals(client.getAttribute(Constants.AGENT_CLIENT_ATTRIBUTE))) {
                    client.removeAttribute(Constants.AGENT_CLIENT_ATTRIBUTE);
                    return true;
                }
            }
            case "servers" -> {
                for (Server server : entries(SERVER, Server.class).toList()) {
                    if (id.equals(server.id())) {
                        ensureUnlinked(id);
                        remove(SERVER, server.resource());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void ensureUnlinked(String id) {
        if (entries(CONNECTION, Connection.class).anyMatch(link -> id.equals(link.agentId()) || id.equals(link.serverId()))) {
            throw new IllegalStateException("Remove the connections that use this entry first");
        }
    }

    private static String connectionKey(String caller, String resource, Mode mode) {
        return String.join("\n", caller, resource, mode.name());
    }

    private static String key(String prefix, String identity) {
        return prefix + HashUtils.sha256UrlEncodedHash(identity, StandardCharsets.UTF_8);
    }

    private void remove(String prefix, String identity) {
        realm.removeAttribute(key(prefix, identity));
    }

    private void write(String prefix, String identity, Object value) {
        try {
            realm.setAttribute(key(prefix, identity), JsonSerialization.writeValueAsString(value));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save agent directory entry", e);
        }
    }

    private <T> T read(String prefix, String identity, Class<T> type) {
        return identity == null ? null : decode(realm.getAttribute(key(prefix, identity)), type);
    }

    private <T> Stream<T> entries(String prefix, Class<T> type) {
        return realm.getAttributes().entrySet().stream().filter(entry -> entry.getKey().startsWith(prefix))
                .sorted(Map.Entry.comparingByKey()).map(entry -> decode(entry.getValue(), type)).filter(Objects::nonNull);
    }

    private static <T> T decode(String value, Class<T> type) {
        try {
            return value == null ? null : JsonSerialization.readValue(value, type);
        } catch (IOException e) {
            throw new IllegalStateException("Invalid agent directory entry", e);
        }
    }
}
