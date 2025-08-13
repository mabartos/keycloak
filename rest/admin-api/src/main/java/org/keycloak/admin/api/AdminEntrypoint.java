package org.keycloak.admin.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.keycloak.admin.api.root.AdminRoot;

@ApplicationScoped
@Path("/admin")
public class AdminEntrypoint {

    @Inject
    AdminRoot adminRoot;

    @Path("/api")
    public AdminRoot adminRoot() {
        return adminRoot;
    }
}
