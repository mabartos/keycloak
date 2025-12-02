package org.keycloak.services.client;

import java.util.Optional;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.models.mapper.MapStructClientModelContext;
import org.keycloak.models.mapper.MapStructModelMapper;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.representations.admin.v2.validation.CreateClientDefault;
import org.keycloak.services.ServiceException;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.services.resources.admin.ClientsResource;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.validation.jakarta.HibernateValidatorProvider;
import org.keycloak.validation.jakarta.JakartaValidatorProvider;

// TODO
public class DefaultClientService implements ClientService {
    private final KeycloakSession session;
    private final ClientModelMapper mapper;
    private final MapStructClientModelContext mapperContext;
    private final JakartaValidatorProvider validator;
    private final ClientsResource clientsResource;
    private final ClientResource clientResource;

    public DefaultClientService(KeycloakSession session, RealmAdminResource realmAdminResource, ClientResource clientResource) {
        this.session = session;
        this.clientResource = clientResource;

        this.clientsResource = realmAdminResource.getClients();
        this.mapper = new MapStructModelMapper().clients();
        this.mapperContext = new MapStructClientModelContext(session, null, realmAdminResource, clientResource);
        this.validator = new HibernateValidatorProvider();
    }

    public DefaultClientService(KeycloakSession session, RealmAdminResource realmAdminResource) {
        this(session, realmAdminResource, null);
    }

    @Override
    public Optional<ClientRepresentation> getClient(RealmModel realm, String clientId, ClientProjectionOptions projectionOptions) {
        return Optional.ofNullable(clientResource).map(ClientResource::viewClientModel).map(model -> mapper.fromModel(model, mapperContext.setRealm(realm)));
    }

    @Override
    public Stream<ClientRepresentation> getClients(RealmModel realm, ClientProjectionOptions projectionOptions,
                                                   ClientSearchOptions searchOptions, ClientSortAndSliceOptions sortAndSliceOptions) {
        return clientsResource.getClientModels(null, true, false, null, null, null).map(model -> mapper.fromModel(model, mapperContext.setRealm(realm)));
    }

    @Override
    public CreateOrUpdateResult createOrUpdate(RealmModel realm, ClientRepresentation client, boolean allowUpdate) throws ServiceException {
        mapperContext.setRealm(realm);

        boolean created = false;
        ClientModel model;
        if (clientResource != null) {
            if (!allowUpdate) {
                throw new ServiceException("Client already exists", Response.Status.CONFLICT);
            }
            model = mapper.toModel(clientResource.viewClientModel(), client, mapperContext);
            var rep = ModelToRepresentation.toRepresentation(model, session);
            clientResource.update(rep);
        } else {
            created = true;
            validator.validate(client, CreateClientDefault.class); // TODO improve it to avoid second validation when we know it is create and not update

            model = mapper.toModel(client, mapperContext);
            var rep = ModelToRepresentation.toRepresentation(model, session);
            model = clientsResource.createClientModel(rep);
        }

        var updated = mapper.fromModel(model, mapperContext);

        return new CreateOrUpdateResult(updated, created);
    }

    @Override
    public Stream<ClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions) {
        // TODO Auto-generated method stub
        return null;
    }

}
