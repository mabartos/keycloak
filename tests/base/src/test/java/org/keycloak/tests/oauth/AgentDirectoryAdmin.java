package org.keycloak.tests.oauth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakUrls;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;

public final class AgentDirectoryAdmin implements AutoCloseable {
    private final Client http = Keycloak.getClientProvider().newRestEasyClient(null, null, false);
    private final WebTarget target;
    private final Deque<WebTarget> created = new ArrayDeque<>();

    public AgentDirectoryAdmin(Keycloak admin, KeycloakUrls urls, ManagedRealm realm) {
        target = http.target(urls.getAdmin()).path("realms").path(realm.getName()).path("ai-agents")
                .register(new BearerAuthFilter(admin.tokenManager()));
    }

    public String put(String collection, Map<String, Object> body) {
        try (Response response = target.path(collection).request().put(Entity.json(body))) {
            Assertions.assertEquals(200, response.getStatus(), () -> response.readEntity(String.class));
            String id = response.readEntity(JsonNode.class).get("id").asText();
            created.push(target.path(collection).path(id));
            return id;
        }
    }

    public JsonNode list() {
        return target.request(MediaType.APPLICATION_JSON).get(JsonNode.class);
    }

    public int delete(String collection, String id) {
        try (Response response = target.path(collection).path(id).request().delete()) {
            return response.getStatus();
        }
    }

    @Override
    public void close() {
        try {
            while (!created.isEmpty()) {
                try (Response response = created.pop().request().delete()) {
                    Assertions.assertTrue(response.getStatus() == 204 || response.getStatus() == 404);
                }
            }
        } finally {
            http.close();
        }
    }
}
