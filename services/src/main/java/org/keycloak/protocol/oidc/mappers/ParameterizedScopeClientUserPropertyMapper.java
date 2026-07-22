package org.keycloak.protocol.oidc.mappers;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.provider.ProviderConfigProperty;

public class ParameterizedScopeClientUserPropertyMapper extends ParameterizedScopeUserPropertyMapper {

    public static final String PROVIDER_ID = "oidc-parameterized-scope-client-user-property-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.USER_ATTRIBUTE);
        property.setLabel(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_LABEL);
        property.setHelpText("Service account user property (e.g. id, email) to map to the token claim. The client is resolved from the parameterized scope parameter (client ID).");
        property.setType(ProviderConfigProperty.USER_PROFILE_ATTRIBUTE_LIST_TYPE);
        configProperties.add(property);

        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, ParameterizedScopeClientUserPropertyMapper.class);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Parameterized Scope Client User Property";
    }

    @Override
    public String getHelpText() {
        return "Resolves a client from a parameterized scope parameter (client ID), obtains its service account user, and maps a user attribute or property to a token claim.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    protected UserModel resolveUser(KeycloakSession keycloakSession, RealmModel realm, String parameterValue) {
        ClientModel client = realm.getClientByClientId(parameterValue);
        if (client != null) {
            return keycloakSession.users().getServiceAccount(client);
        }
        return null;
    }

    public static ProtocolMapperModel create(String name, String userAttribute,
                                              String tokenClaimName, String claimType,
                                              boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        return create(name, userAttribute, tokenClaimName, claimType, accessToken, idToken, introspectionEndpoint, false);
    }

    public static ProtocolMapperModel create(String name, String userAttribute,
                                              String tokenClaimName, String claimType,
                                              boolean accessToken, boolean idToken, boolean introspectionEndpoint,
                                              boolean multivalued) {
        ProtocolMapperModel mapper = OIDCAttributeMapperHelper.createClaimMapper(
                name, userAttribute, tokenClaimName, claimType,
                accessToken, idToken, false, introspectionEndpoint,
                PROVIDER_ID);
        mapper.getConfig().put(ProtocolMapperUtils.MULTIVALUED, Boolean.toString(multivalued));
        return mapper;
    }
}
