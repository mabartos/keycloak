package org.keycloak.admin.api.root;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.keycloak.admin.api.ChosenBySpi;
import org.keycloak.services.resources.admin.AdminCorsPreflightService;

@RequestScoped
@ChosenBySpi
public class DefaultAdminRoot implements AdminRoot {

    @Inject
    AdminApi adminApi;

    @Override
    @Path("")
    public AdminApi latestAdminApi() {
        return adminApiV2();
    }

    @Override
    @Path("/latest-version")
    public String latestVersion() {
        return "v2";
    }

    @Path("/v2")
    public AdminApi adminApiV2() {
        return adminApi;
    }

    @Path("{any:.*}")
    @OPTIONS
    @Operation(hidden = true)
    public Object preFlight() {
        return new AdminCorsPreflightService();
    }

    @Override
    public void close() {

    }
}
