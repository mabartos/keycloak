package org.keycloak.models.mapper;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.ServiceException;
import org.keycloak.services.resources.admin.ClientResource;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ObjectFactory;

@Mapper
public interface MapStructClientModelMapper extends ClientModelMapper {

    @Override
    @ModelToRep
    ClientRepresentation fromModel(ClientModel model, @Context ModelMapperContext context);

    // we don't want to ignore nulls so that we completely overwrite the state
    @Override
    @RepToModel
    ClientModel toModel(@MappingTarget ClientModel existingModel, ClientRepresentation rep, @Context ModelMapperContext context) throws ServiceException;

    @Override
    @RepToModel
    ClientModel toModel(ClientRepresentation rep, @Context ModelMapperContext context) throws ServiceException;

    /*-------------------------------------*
     *              MAPPERS                *
     *-------------------------------------*/
    @Mapping(target = "name", source = "displayName")
    @Mapping(target = "baseUrl", source = "appUrl")
    @Mapping(target = "redirectUris", source = "appRedirectUrls")
    @Mapping(target = "authenticationFlowBindingOverrides", source = "loginFlows", ignore = true) // TODO
    @Mapping(target = "publicClient", source = "auth.enabled", qualifiedByName = "isPublicClientPrimitive")
    @Mapping(target = "clientAuthenticatorType", source = "auth.method")
    @Mapping(target = "secret", source = "auth.secret")
    @Mapping(target = "serviceAccountsEnabled", source = "serviceAccount.enabled")
    @interface RepToModel {
    }

    @Mapping(target = "displayName", source = "name")
    @Mapping(target = "appUrl", source = "baseUrl")
    @Mapping(target = "appRedirectUrls", source = "redirectUris")
    @Mapping(target = "loginFlows", source = "authenticationFlowBindingOverrides", ignore = true)
    @Mapping(target = "auth.enabled", source = "publicClient", qualifiedByName = "isPublicClient")
    @Mapping(target = "auth.method", source = "clientAuthenticatorType")
    @Mapping(target = "auth.secret", source = "secret")
    @Mapping(target = "auth.certificate", ignore = true) // no cert in the representation
    @Mapping(target = "roles", source = "rolesStream", qualifiedByName = "getRoleStrings")
    @Mapping(target = "serviceAccount.enabled", source = "serviceAccountsEnabled")
    @Mapping(target = "serviceAccount.roles", source = ".", qualifiedByName = "getServiceAccountRoles")
    @interface ModelToRep {
    }

    /*-------------------------------------*
     *          HELPER METHODS             *
     *-------------------------------------*/
    @ObjectFactory
    default ClientModel createClientModel(ClientRepresentation rep, @Context ModelMapperContext context) {
        // dummy add/remove to obtain a detached model
        var realm = ((MapStructClientModelContext) context).getRealm().orElseThrow(() -> new IllegalArgumentException("You need to specify the realm in the mapper context"));
        var model = realm.addClient(rep.getClientId());
        realm.removeClient(model.getId());
        return model;
    }

    // reusing the v1 logic
    @AfterMapping
    default void addRoles(@MappingTarget ClientModel model, ClientRepresentation rep, @Context ModelMapperContext context) {
        var mapperContext = (MapStructClientModelContext) context;
        var clientResource = getFreshClientResource(mapperContext, rep.getClientId());

        if (clientResource.isEmpty()) return;
        var roleResource = clientResource.get().getRoleContainerResource();

        // Create a client role if it does not exist. If the client is removed, even the roles should be removed due to the cascading.
        Optional.ofNullable(rep.getRoles())
                .orElse(Collections.emptySet())
                .stream()
                .filter(role -> {
                    try {
                        return roleResource.getRole(role) == null;
                    } catch (NotFoundException e) {
                        return true;
                    }
                })
                .forEach(model::addRole);
    }

    // reusing the v1 logic
    @AfterMapping
    default void handleServiceAccount(@MappingTarget ClientModel model, ClientRepresentation rep, @Context ModelMapperContext context) {
        var mapperContext = (MapStructClientModelContext) context;
        var clientResource = getFreshClientResource(mapperContext, rep.getClientId());

        var serviceAccount = rep.getServiceAccount();
        if (serviceAccount != null && serviceAccount.getEnabled() != null) {
            ClientResource.updateClientServiceAccount(mapperContext.getSession(), model, serviceAccount.getEnabled());

            if (serviceAccount.getEnabled() && !serviceAccount.getRoles().isEmpty() && clientResource.isPresent()) {
                var roleMappers = mapperContext.getRealmAdminResource().users().user(clientResource.get().getServiceAccountUser().getId()).getRoleMappings();
                var rolesList = serviceAccount.getRoles().stream()
                        .map(roleName -> new RoleRepresentation(roleName, "", false))
                        .toList();
                try {
                    roleMappers.addRealmRoleMappings(rolesList);
                } catch (NotFoundException e) {
                    throw new ServiceException("Cannot assign role to the service account (field 'serviceAccount.roles') as it does not exist", Response.Status.BAD_REQUEST);
                }
            }
        }
    }

    private Optional<ClientResource> getFreshClientResource(MapStructClientModelContext context, String clientId) {
        return context.getClientResource().or(() -> {
            try {
                return Optional.of(context.getRealmAdminResource().getClients().getClient(clientId));
            } catch (ClientErrorException e) {
                return Optional.empty();
            }
        });

    }

    @Named("isPublicClientPrimitive")
    default boolean isPublicClientPrimitive(Boolean authEnabled) {
        var result = isPublicClient(authEnabled);
        return result != null ? result : false;
    }

    @Named("isPublicClient")
    default Boolean isPublicClient(Boolean authEnabled) {
        return authEnabled != null ? !authEnabled : null;
    }

    @Named("getRoleStrings")
    default Set<String> getRoleStrings(Stream<RoleModel> stream) {
        return stream.map(RoleModel::getName).collect(Collectors.toSet());
    }

    @Named("getServiceAccountRoles")
    default Set<String> getServiceAccountRoles(ClientModel client, @Context ModelMapperContext context) {
        var session = ((MapStructClientModelContext) context).getSession();
        if (client.isServiceAccountsEnabled()) {
            return session.users().getServiceAccount(client)
                    .getRoleMappingsStream()
                    .map(RoleModel::getName)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
}
