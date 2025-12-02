package org.keycloak.models.mapper;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.ServiceException;

public interface ClientModelMapper {

    ClientRepresentation fromModel(ClientModel model, ModelMapperContext context);

    ClientModel toModel(ClientModel existingModel, ClientRepresentation rep, ModelMapperContext context) throws ServiceException;

    ClientModel toModel(ClientRepresentation rep, ModelMapperContext context) throws ServiceException;
}
