package org.keycloak.admin.api;

import jakarta.ws.rs.Path;

import org.keycloak.admin.api.client.ClientsApiGroup;

public interface AdminApi {

    String CONTENT_TYPE_MERGE_PATCH = "application/merge-patch+json";

    @Path("clients")
    ClientsApiGroup clientsGroupDefault();

    @Path("clients/v2")
    ClientsApiGroup clientsGroupV2();
}
