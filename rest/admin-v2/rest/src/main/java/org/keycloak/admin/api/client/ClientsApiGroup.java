package org.keycloak.admin.api.client;

import jakarta.ws.rs.Path;

public interface ClientsApiGroup {

    @Path("clients")
    ClientsApi clients();
}
