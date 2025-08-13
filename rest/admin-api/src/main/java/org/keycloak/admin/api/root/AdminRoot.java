package org.keycloak.admin.api.root;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.keycloak.provider.Provider;

public interface AdminRoot extends Provider {

    AdminApi latestAdminApi();

    @GET
    @Path("/latest-version")
    String latestVersion();
}
