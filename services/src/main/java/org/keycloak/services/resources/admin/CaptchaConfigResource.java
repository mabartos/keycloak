package org.keycloak.services.resources.admin;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.captcha.CaptchaConfigService;
import org.keycloak.authentication.captcha.CaptchaInstance;
import org.keycloak.authentication.captcha.CaptchaProvider;
import org.keycloak.authentication.captcha.CaptchaProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ComponentTypeRepresentation;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

/**
 * Admin REST resource for managing realm-level CAPTCHA configuration.
 *
 * <p>Supports multiple configured CAPTCHA provider instances per realm,
 * with one set as the default. Instances are stored as components.</p>
 */
public class CaptchaConfigResource {

    private final KeycloakSession session;
    private final CaptchaConfigService configService;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public CaptchaConfigResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.configService = new CaptchaConfigService(session);
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    // ---- Available provider types ----

    /**
     * List all available CAPTCHA provider factories with their configuration properties.
     */
    @GET
    @Path("providers")
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<ComponentTypeRepresentation> getProviders() {
        auth.realm().requireViewRealm();

        return session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(CaptchaProvider.class)
                .map(this::toComponentType);
    }

    /**
     * Get a specific CAPTCHA provider factory with its configuration properties.
     */
    @GET
    @Path("providers/{provider_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentTypeRepresentation getProvider(@PathParam("provider_id") String providerId) {
        auth.realm().requireViewRealm();

        ProviderFactory<?> factory = session.getKeycloakSessionFactory()
                .getProviderFactory(CaptchaProvider.class, providerId);

        if (factory == null) {
            throw new NotFoundException("CAPTCHA provider not found: " + providerId);
        }

        return toComponentType(factory);
    }

    // ---- Configured instances ----

    /**
     * List all configured CAPTCHA provider instances for this realm.
     */
    @GET
    @Path("instances")
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<CaptchaInstance> getInstances() {
        auth.realm().requireViewRealm();

        return configService.getInstances();
    }

    /**
     * Get a configured CAPTCHA provider instance by ID.
     */
    @GET
    @Path("instances/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public CaptchaInstance getInstance(@PathParam("id") String id) {
        auth.realm().requireViewRealm();

        CaptchaInstance instance = configService.getInstance(id);
        if (instance == null) {
            throw new NotFoundException("CAPTCHA instance not found: " + id);
        }

        return instance;
    }

    /**
     * Create a new CAPTCHA provider instance.
     */
    @POST
    @Path("instances")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createInstance(CaptchaInstance instance) {
        auth.realm().requireManageRealm();

        try {
            configService.validateInstance(instance);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }

        CaptchaInstance created = configService.addInstance(instance);

        adminEvent.operation(OperationType.CREATE)
                .resourcePath(session.getContext().getUri(), created.getId())
                .representation(created)
                .success();

        return Response.created(
                session.getContext().getUri().getAbsolutePathBuilder()
                        .path(created.getId()).build()
        ).entity(created).build();
    }

    /**
     * Update an existing CAPTCHA provider instance.
     */
    @PUT
    @Path("instances/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateInstance(@PathParam("id") String id, CaptchaInstance instance) {
        auth.realm().requireManageRealm();

        try {
            configService.validateInstance(instance);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }

        CaptchaInstance updated = configService.updateInstance(id, instance);
        if (updated == null) {
            throw new NotFoundException("CAPTCHA instance not found: " + id);
        }

        adminEvent.operation(OperationType.UPDATE)
                .resourcePath(session.getContext().getUri())
                .representation(updated)
                .success();

        return Response.noContent().build();
    }

    /**
     * Delete a CAPTCHA provider instance.
     */
    @DELETE
    @Path("instances/{id}")
    public Response deleteInstance(@PathParam("id") String id) {
        auth.realm().requireManageRealm();

        if (!configService.removeInstance(id)) {
            throw new NotFoundException("CAPTCHA instance not found: " + id);
        }

        adminEvent.operation(OperationType.DELETE)
                .resourcePath(session.getContext().getUri())
                .success();

        return Response.noContent().build();
    }

    // ---- Default instance ----

    /**
     * Get the ID of the default CAPTCHA instance.
     */
    @GET
    @Path("default")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getDefault() {
        auth.realm().requireViewRealm();

        String id = configService.getDefaultInstanceId();
        Map<String, String> result = new HashMap<>();
        result.put("id", id);
        return result;
    }

    /**
     * Set the default CAPTCHA instance by ID.
     */
    @PUT
    @Path("default")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setDefault(Map<String, String> body) {
        auth.realm().requireManageRealm();

        String id = body.get("id");
        if (id == null || id.isEmpty()) {
            configService.clearDefault();
        } else {
            try {
                configService.setDefaultInstanceId(id);
            } catch (IllegalArgumentException e) {
                throw new NotFoundException(e.getMessage());
            }
        }

        adminEvent.operation(OperationType.UPDATE)
                .resourcePath(session.getContext().getUri())
                .representation(body)
                .success();

        return Response.noContent().build();
    }

    // ---- Helpers ----

    private ComponentTypeRepresentation toComponentType(ProviderFactory<?> factory) {
        ComponentTypeRepresentation rep = new ComponentTypeRepresentation();
        rep.setId(factory.getId());

        if (factory instanceof CaptchaProviderFactory) {
            CaptchaProviderFactory captchaFactory = (CaptchaProviderFactory) factory;
            rep.setHelpText(captchaFactory.getHelpText());

            List<ProviderConfigProperty> configProperties = captchaFactory.getConfigProperties();
            if (configProperties == null) {
                configProperties = Collections.emptyList();
            }
            rep.setProperties(ModelToRepresentation.toRepresentation(configProperties));
        }

        return rep;
    }
}
