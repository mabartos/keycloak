package org.keycloak.admin.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.fabric8.zjsonpatch.JsonPatch;
import io.fabric8.zjsonpatch.JsonPatchException;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.api.FieldValidation;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServiceException;
import org.keycloak.services.client.ClientService;

import java.io.IOException;
import java.util.Objects;

public class ClientApiV2 {
    public static final String CONTENT_TYPE_MERGE_PATCH = "application/merge-patch+json";

    private final KeycloakSession session;
    private final RealmModel realm;
    private final ClientModel client;
    private final ClientService clientService;
    private HttpResponse response;

    public ClientApiV2(KeycloakSession session) {
        this.session = session;
        this.realm = Objects.requireNonNull(session.getContext().getRealm());
        this.client = Objects.requireNonNull(session.getContext().getClient());
        this.clientService = session.getProvider(ClientService.class);
        this.response = session.getContext().getHttpResponse();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation getClient() {
        return clientService.getClient(realm, client.getClientId(), null)
                .orElseThrow(() -> new NotFoundException("Cannot find the specified client"));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation createOrUpdateClient(@Valid ClientRepresentation client, @QueryParam("fieldValidation") FieldValidation fieldValidation) {
        try {
            var result = clientService.createOrUpdate(realm, client, true);
            if (result.created()) {
                response.setStatus(Response.Status.CREATED.getStatusCode());
            }
            return result.representation();
        } catch (ServiceException e) {
            throw new WebApplicationException(e.getMessage(), e.getSuggestedResponseStatus().orElse(Response.Status.BAD_REQUEST));
        }
    }

    @PATCH
    @Consumes({MediaType.APPLICATION_JSON_PATCH_JSON, CONTENT_TYPE_MERGE_PATCH})
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation patchClient(JsonNode patch, @QueryParam("fieldValidation") FieldValidation fieldValidation) {
        // patches don't yet allow for creating
        ClientRepresentation client = getClient();
        try {
            String contentType = session.getContext().getHttpRequest().getHttpHeaders().getHeaderString(HttpHeaders.CONTENT_TYPE);

            ClientRepresentation updated = null;

            // TODO: there should be a more centralized objectmapper
            ObjectMapper objectMapper = new ObjectMapper();
            if (MediaType.valueOf(contentType).getSubtype().equals(MediaType.APPLICATION_JSON_PATCH_JSON_TYPE.getSubtype())) {
                JsonNode patchedNode = JsonPatch.apply(patch, objectMapper.convertValue(client, JsonNode.class));
                updated = objectMapper.convertValue(patchedNode, ClientRepresentation.class);
            } else { // must be merge patch
                final ObjectReader objectReader = objectMapper.readerForUpdating(client);
                updated = objectReader.readValue(patch);
            }

            // TODO: reuse in the other methods
            if (!updated.getAdditionalFields().isEmpty()) {
                if (fieldValidation == null || fieldValidation == FieldValidation.Strict) {
                    // validation failed
                    throw new WebApplicationException("Payload contains unknown fields: " + updated.getAdditionalFields().keySet(), Response.Status.BAD_REQUEST);
                } else if (fieldValidation == FieldValidation.Warn) {
                    response.addHeader("WARNING", "Payload contains unknown fields: " + updated.getAdditionalFields().keySet());
                }
            }
            return clientService.createOrUpdate(realm, updated, true).representation();
        } catch (JsonPatchException e) {
            // TODO: kubernetes uses 422 instead
            throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (JsonProcessingException e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (IOException e) {
            throw ErrorResponse.error("Unknown Error Occurred", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
