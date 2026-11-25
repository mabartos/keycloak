/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.oidc.mappers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.scope.ClientDelegationScopeType;
import org.keycloak.protocol.oidc.scope.DelegationScopeType;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;

import static org.keycloak.OAuth2Constants.CLIENT_ID;
import static org.keycloak.representations.JsonWebToken.SUBJECT;
import static org.keycloak.utils.JsonUtils.splitClaimPath;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCAttributeMapperHelper {

    public static final String TOKEN_CLAIM_NAME = "claim.name";
    public static final String TOKEN_CLAIM_NAME_LABEL = "tokenClaimName.label";
    public static final String TOKEN_CLAIM_NAME_TOOLTIP = "tokenClaimName.tooltip";
    public static final String JSON_TYPE = "jsonType.label";
    public static final String JSON_TYPE_TOOLTIP = "jsonType.tooltip";
    public static final String INCLUDE_IN_ACCESS_TOKEN = "access.token.claim";
    public static final String INCLUDE_IN_ACCESS_TOKEN_LABEL = "includeInAccessToken.label";
    public static final String INCLUDE_IN_ACCESS_TOKEN_HELP_TEXT = "includeInAccessToken.tooltip";
    public static final String INCLUDE_IN_ID_TOKEN = "id.token.claim";
    public static final String INCLUDE_IN_ID_TOKEN_LABEL = "includeInIdToken.label";
    public static final String INCLUDE_IN_ID_TOKEN_HELP_TEXT = "includeInIdToken.tooltip";
    public static final String INCLUDE_IN_ACCESS_TOKEN_RESPONSE = "access.tokenResponse.claim";
    public static final String INCLUDE_IN_ACCESS_TOKEN_RESPONSE_LABEL = "includeInAccessTokenResponse.label";
    public static final String INCLUDE_IN_ACCESS_TOKEN_RESPONSE_HELP_TEXT = "includeInAccessTokenResponse.tooltip";

    public static final String INCLUDE_IN_USERINFO = "userinfo.token.claim";
    public static final String INCLUDE_IN_USERINFO_LABEL = "includeInUserInfo.label";
    public static final String INCLUDE_IN_USERINFO_HELP_TEXT = "includeInUserInfo.tooltip";

    public static final String INCLUDE_IN_INTROSPECTION = "introspection.token.claim";
    public static final String INCLUDE_IN_INTROSPECTION_LABEL = "includeInIntrospection.label";
    public static final String INCLUDE_IN_INTROSPECTION_HELP_TEXT = "includeInIntrospection.tooltip";

    public static final String INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN = "lightweight.claim";

    public static final String INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN_LABEL = "includeInLightweight.label";

    public static final String INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN_HELP_TEXT = "includeInLightweight.tooltip";

    private static final Logger logger = Logger.getLogger(OIDCAttributeMapperHelper.class);

    /**
     * Interface for a token property setter in a class T that accept claims.
     * @param <T> The token class for the property
     */
    private static interface PropertySetter<T> {
        void set(String claim, String mapperName, T token, Object value);
    }

    /**
     * Setters for claims in IDToken/AccessToken that will not use the other claims map.
     */
    private static final Map<String, PropertySetter<IDToken>> tokenPropertySetters;

    /**
     * Setters for claims in AccessTokenResponse that will not use the other claims map.
     */
    private static final Map<String, PropertySetter<AccessTokenResponse>> responsePropertySetters;

    private static final Set<String> RESERVED_DELEGATION_CLAIMS = Set.of(IDToken.ACT, IDToken.MAY_ACT);
    private static final Map<String, Set<String>> MAY_ACT_MAPPER_EXCEPTIONS = Map.of(
            IDToken.ACT, Set.of(),
            IDToken.MAY_ACT, Set.of(),
            SUBJECT, Set.of(ParameterizedScopeClientSubMapper.PROVIDER_ID, ParameterizedScopeUserPropertyMapper.PROVIDER_ID),
            CLIENT_ID, Set.of(ParameterizedScopeMapper.PROVIDER_ID));

    static {
        // allowed claims that can be set in the IDToken/AccessToken object
        Map<String, PropertySetter<IDToken>> tmpToken = new HashMap<>();
        tmpToken.put("sub", (claim, mapperName, token, value) -> {
            token.setSubject(value.toString());
        });
        tmpToken.put("azp", (claim, mapperName, token, value) -> {
            token.issuedFor(value.toString());
        });
        tmpToken.put(IDToken.ACR, (claim, mapperName, token, value) -> {
            token.setAcr(value.toString());
        });
        tmpToken.put(IDToken.AUTH_TIME, (claim, mapperName, token, value) -> {
            try {
                token.setAuth_time(Long.parseLong(value.toString()));
            } catch (NumberFormatException ignored){

            }
        });
        tmpToken.put("aud", (claim, mapperName, token, value) -> {
            if (value instanceof Collection) {
                String[] audiences = ((Collection<?>) value).stream().map(Object::toString).toArray(String[]::new);
                token.audience(audiences);
            } else {
                token.audience(value.toString());
            }
        });
        // not allowed claims that are set by the server and can generate duplicates
        PropertySetter<IDToken> notAllowedInToken = (claim, mapperName, token, value) -> {
            logger.warnf("Claim '%s' is non-modifiable in IDToken. Ignoring the assignment for mapper '%s'.", claim, mapperName);
        };
        tmpToken.put("jti", notAllowedInToken);
        tmpToken.put("typ", notAllowedInToken);
        tmpToken.put("iat", notAllowedInToken);
        tmpToken.put("exp", notAllowedInToken);
        tmpToken.put("iss", notAllowedInToken);
        tmpToken.put("scope", notAllowedInToken);
        tmpToken.put(IDToken.NONCE, notAllowedInToken);
        tmpToken.put(IDToken.SESSION_STATE, notAllowedInToken);
        RESERVED_DELEGATION_CLAIMS.forEach(claim -> tmpToken.put(claim, notAllowedInToken));
        tokenPropertySetters = Collections.unmodifiableMap(tmpToken);

        // in the AccessTokenResponse do not allow modifications for server assigned properties
        Map<String, PropertySetter<AccessTokenResponse>> tmpResponse = new HashMap<>();
        PropertySetter<AccessTokenResponse> notAllowedInResponse = (claim, mapperName, token, value) -> {
            logger.warnf("Claim '%s' is non-modifiable in AccessTokenResponse. Ignoring the assignment for mapper '%s'.", claim, mapperName);
        };
        tmpResponse.put("access_token", notAllowedInResponse);
        tmpResponse.put("token_type", notAllowedInResponse);
        tmpResponse.put("session_state", notAllowedInResponse);
        tmpResponse.put("expires_in", notAllowedInResponse);
        tmpResponse.put("id_token", notAllowedInResponse);
        tmpResponse.put("refresh_token", notAllowedInResponse);
        tmpResponse.put("refresh_expires_in", notAllowedInResponse);
        tmpResponse.put("not-before-policy", notAllowedInResponse);
        tmpResponse.put("scope", notAllowedInResponse);
        RESERVED_DELEGATION_CLAIMS.forEach(claim -> tmpResponse.put(claim, notAllowedInResponse));
        responsePropertySetters = Collections.unmodifiableMap(tmpResponse);
    }

    public static Object mapAttributeValue(ProtocolMapperModel mappingModel, Object attributeValue) {
        if (attributeValue == null) return null;

        if (attributeValue instanceof Collection) {
            Collection<?> valueAsList = (Collection<?>) attributeValue;
            if (valueAsList.isEmpty()) return null;

            if (isMultivalued(mappingModel)) {
                List<Object> result = new ArrayList<>();
                for (Object valueItem : valueAsList) {
                    result.add(mapAttributeValue(mappingModel, valueItem));
                }
                return result;
            } else {
                if (valueAsList.size() > 1) {
                    ServicesLogger.LOGGER.multipleValuesForMapper(attributeValue.toString(), mappingModel.getName());
                }

                attributeValue = valueAsList.iterator().next();
            }
        }

        String type = mappingModel.getConfig().get(JSON_TYPE);
        return convertToType(type, attributeValue);
    }

    private static <X, T> List<T> transform(List<X> attributeValue, Function<X, T> mapper) {
        return attributeValue.stream()
                .filter(Objects::nonNull)
                .map(mapper)
                .collect(Collectors.toList());
    }

    private static Object convertToType(String type, Object attributeValue) {
        if (type == null || attributeValue == null) return attributeValue;
        switch (type) {
            case "boolean":
                Boolean booleanObject = getBoolean(attributeValue);
                if (booleanObject != null) return booleanObject;
                if (attributeValue instanceof List) {
                    return transform((List<?>) attributeValue, OIDCAttributeMapperHelper::getBoolean);
                }
                return null;
            case "String":
                if (attributeValue instanceof String) return attributeValue;
                if (attributeValue instanceof List) {
                    return transform((List<?>) attributeValue, OIDCAttributeMapperHelper::getString);
                }
                return attributeValue.toString();
            case "long":
                Long longObject = getLong(attributeValue);
                if (longObject != null) return longObject;
                if (attributeValue instanceof List) {
                    return transform((List<?>) attributeValue, OIDCAttributeMapperHelper::getLong);
                }
                return null;
            case "int":
                Integer intObject = getInteger(attributeValue);
                if (intObject != null) return intObject;
                if (attributeValue instanceof List) {
                    return transform((List<?>) attributeValue, OIDCAttributeMapperHelper::getInteger);
                }
                return null;
            case "JSON":
                JsonNode jsonNodeObject = getJsonNode(attributeValue);
                if (jsonNodeObject != null) return jsonNodeObject;
                if (attributeValue instanceof List) {
                    return transform((List<?>) attributeValue, OIDCAttributeMapperHelper::getJsonNode);
                }
                return null;
            default:
                return attributeValue;
        }
    }

    private static String getString(Object attributeValue) {
        return attributeValue.toString();
    }


    private static Long getLong(Object attributeValue) {
        if (attributeValue instanceof Long) return (Long) attributeValue;
        if (attributeValue instanceof String) {
            try {
                return Long.valueOf((String) attributeValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Integer getInteger(Object attributeValue) {
        if (attributeValue instanceof Integer) return (Integer) attributeValue;
        if (attributeValue instanceof String) {
            try {
                return Integer.valueOf((String) attributeValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Boolean getBoolean(Object attributeValue) {
        if (attributeValue instanceof Boolean) return (Boolean) attributeValue;
        if (attributeValue instanceof String) return Boolean.valueOf((String) attributeValue);
        return null;
    }

    private static JsonNode getJsonNode(Object attributeValue) {
        if (attributeValue instanceof JsonNode){
            return (JsonNode) attributeValue;
        }
        if (attributeValue instanceof Map) {
            try {
                return JsonSerialization.createObjectNode(attributeValue);
            } catch (Exception ignore) {
            }
        }
        if (attributeValue instanceof String) {
            try {
                return JsonSerialization.readValue(attributeValue.toString(), JsonNode.class);
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    /**
     * Get or initialize the organization claim as a mutable Map for composition between organization mappers.
     * Handles conversion from OrganizationMembershipMapper output to Map structure that
     * OrganizationGroupMembershipMapper can add to. Supports ObjectNode (JSON type), Collection or String
     * (String type), existing Map, or creates empty Map if null.
     *
     * @param token the token
     * @param effectiveModel the effective model of the protocol mapper to retrieve the name of the organization claim
     * @return a mutable Map that can be manipulated; changes will be reflected in the token
     */
    public static Map<String, Object> getOrInitializeOrganizationClaimAsMap(IDToken token, ProtocolMapperModel effectiveModel) {
        List<String> claimPath = writableClaimPath(effectiveModel.getConfig().get(TOKEN_CLAIM_NAME), effectiveModel);
        Object existingClaim = getNestedClaimValue(token.getOtherClaims(), claimPath);
        Map<String, Object> result;

        if (existingClaim instanceof ObjectNode) {
            // OrganizationMembershipMapper with JSON_TYPE="JSON"
            result = JsonSerialization.mapper.convertValue(existingClaim, Map.class);
        } else if (existingClaim instanceof Collection || existingClaim instanceof String) {
            // OrganizationMembershipMapper with String type - single alias or Collection of aliases
            result = new HashMap<>();
            Stream<?> items = existingClaim instanceof Collection ? ((Collection<?>) existingClaim).stream() : Stream.of(existingClaim);
            items.filter(Objects::nonNull).forEach(item -> result.put(item.toString(), new HashMap<>()));
        } else if (existingClaim instanceof Map) {
            // Already a Map, use as-is
            result = (Map<String, Object>) existingClaim;
        } else {
            result = new HashMap<>();
        }

        OIDCAttributeMapperHelper.mapClaim(token, effectiveModel, result);
        return result;
    }

    private static Object getNestedClaimValue(Map<String, Object> claims, List<String> path) {
        if (path.isEmpty()) return null;
        Map<String, Object> current = claims;
        for (int i = 0; i < path.size() - 1; i++) {
            Object next = current.get(path.get(i));
            if (!(next instanceof Map)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> nested = (Map<String, Object>) next;
            current = nested;
        }
        return current.get(path.get(path.size() - 1));
    }

    public static void validateClaimName(ProtocolMapperContainerModel container, ProtocolMapperModel mapper) throws ProtocolMapperConfigException {
        List<String> path = configuredClaimPath(mapper);
        if (isReservedDelegationClaim(path) && !isAllowedMayActClaim(path, mapper, container)) {
            throw new ProtocolMapperConfigException("Claim '" + mapper.getConfig().get(TOKEN_CLAIM_NAME) + "' is reserved for delegation");
        }
    }

    public static boolean isTokenMapperAllowed(ProtocolMapperModel mapper, ClientScopeModel scope) {
        List<String> path = configuredClaimPath(mapper);
        return !isReservedDelegationClaim(path) || (Profile.isFeatureEnabled(Profile.Feature.TOKEN_EXCHANGE_DELEGATION)
                && scope.isAlwaysConsent() && isAllowedMayActClaim(path, mapper, scope));
    }

    private static List<String> configuredClaimPath(ProtocolMapperModel mapper) {
        String claim = mapper.getConfig() == null ? null : mapper.getConfig().get(TOKEN_CLAIM_NAME);
        return claim == null ? List.of() : splitClaimPath(claim);
    }

    private static boolean isReservedDelegationClaim(List<String> path) {
        return !path.isEmpty() && RESERVED_DELEGATION_CLAIMS.contains(path.get(0));
    }

    private static boolean isAllowedMayActClaim(List<String> path, ProtocolMapperModel mapper, ProtocolMapperContainerModel container) {
        if (!isAllowedMayActPath(path, mapper) || !(container instanceof ClientScopeModel scope) || scope instanceof ClientModel) {
            return false;
        }
        String scopeType = scope.getParameterizedScopeType();
        String member = path.get(1);
        if (ClientDelegationScopeType.TYPE.equals(scopeType) && OIDCLoginProtocolFactory.CLIENT_DELEGATION_SCOPE.equals(scope.getName())) {
            return !SUBJECT.equals(member) || ParameterizedScopeClientSubMapper.PROVIDER_ID.equals(mapper.getProtocolMapper());
        }
        return DelegationScopeType.TYPE.equals(scopeType) && OIDCLoginProtocolFactory.USER_DELEGATION_SCOPE.equals(scope.getName())
                && !CLIENT_ID.equals(member) && (!SUBJECT.equals(member) || ParameterizedScopeUserPropertyMapper.PROVIDER_ID.equals(mapper.getProtocolMapper()));
    }

    private static boolean isAllowedMayActPath(List<String> path, ProtocolMapperModel mapper) {
        if (path.size() < 2 || !IDToken.MAY_ACT.equals(path.get(0))) {
            return false;
        }

        String member = path.get(1);
        Set<String> allowedMapperIds = MAY_ACT_MAPPER_EXCEPTIONS.get(member);
        if (allowedMapperIds == null) {
            // The exchange validates the optional issuer against the realm.
            return !OIDCLoginProtocol.ISSUER.equals(member) || (path.size() == 2 && "String".equals(mapper.getConfig().get(JSON_TYPE)));
        }
        String provider = mapper.getProtocolMapper();
        return provider != null && allowedMapperIds.contains(provider) && path.size() == 2
                && "String".equals(mapper.getConfig().get(JSON_TYPE))
                && (!ParameterizedScopeUserPropertyMapper.PROVIDER_ID.equals(provider)
                        || UserModel.ID.equals(mapper.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE)));
    }

    static List<String> writableClaimPath(String claimName, ProtocolMapperModel mapper) {
        List<String> path = splitClaimPath(claimName);
        // Substituted paths must not introduce a reserved target after scope validation.
        return isReservedDelegationClaim(path) && (!path.equals(configuredClaimPath(mapper)) || !isAllowedMayActPath(path, mapper))
                ? List.of() : path;
    }

    public static void mapClaim(IDToken token, ProtocolMapperModel mappingModel, Object attributeValue) {
        mapClaim(token, mappingModel, attributeValue, tokenPropertySetters, token.getOtherClaims());
    }

    public static void mapClaim(AccessTokenResponse token, ProtocolMapperModel mappingModel, Object attributeValue) {
        mapClaim(token, mappingModel, attributeValue, responsePropertySetters, token.getOtherClaims());
    }

    private static <T> void mapClaim(T token, ProtocolMapperModel mappingModel, Object attributeValue,
                                     Map<String, PropertySetter<T>> setters, Map<String, Object> jsonObject) {
        attributeValue = mapAttributeValue(mappingModel, attributeValue);
        if (attributeValue == null) {
            return;
        }

        String protocolClaim = mappingModel.getConfig().get(TOKEN_CLAIM_NAME);
        if (protocolClaim == null) {
            return;
        }

        List<String> split = splitClaimPath(protocolClaim);
        if (split.isEmpty()) {
            return;
        }

        String firstClaim = split.iterator().next();
        PropertySetter<T> setter = setters.get(firstClaim);
        if (setter != null && !isAllowedMayActPath(split, mappingModel)) {
            // assign using the property setters over the token
            if (split.size() > 1) {
                logger.warnf("Claim '%s' contains more than one level in a setter. Ignoring the assignment for mapper '%s'.",
                        protocolClaim, mappingModel.getName());
                return;
            }

            setter.set(protocolClaim, mappingModel.getName(), token, attributeValue);
            return;
        }

        // map value to the other claims map
        JsonUtils.mapClaim(split, attributeValue, jsonObject, isMultivalued(mappingModel));
    }

    public static ProtocolMapperModel createClaimMapper(String name,
                                                        String userAttribute,
                                                        String tokenClaimName, String claimType,
                                                        boolean accessToken, boolean idToken, boolean introspectionEndpoint,
                                                        String mapperId) {
        return createClaimMapper(name, userAttribute, tokenClaimName, claimType, accessToken, idToken, true, introspectionEndpoint, mapperId);
    }

    public static ProtocolMapperModel createClaimMapper(String name,
                                                        String userAttribute,
                                                        String tokenClaimName, String claimType,
                                                        boolean accessToken, boolean idToken, boolean userinfo, boolean introspectionEndpoint,
                                                        String mapperId) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(mapperId);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE, userAttribute);
        config.put(TOKEN_CLAIM_NAME, tokenClaimName);
        config.put(JSON_TYPE, claimType);
        if (accessToken) config.put(INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken) config.put(INCLUDE_IN_ID_TOKEN, "true");
        if (userinfo) config.put(INCLUDE_IN_USERINFO, "true");
        if (introspectionEndpoint) config.put(INCLUDE_IN_INTROSPECTION, "true");
        mapper.setConfig(config);
        return mapper;
    }

    public static boolean includeInIDToken(ProtocolMapperModel mappingModel) {
        return "true".equals(mappingModel.getConfig().get(INCLUDE_IN_ID_TOKEN));
    }

    public static boolean includeInAccessToken(ProtocolMapperModel mappingModel) {
        return "true".equals(mappingModel.getConfig().get(INCLUDE_IN_ACCESS_TOKEN));
    }

    public static boolean includeInAccessTokenResponse(ProtocolMapperModel mappingModel) {
        return "true".equals(mappingModel.getConfig().get(INCLUDE_IN_ACCESS_TOKEN_RESPONSE));
    }

    public static boolean isMultivalued(ProtocolMapperModel mappingModel) {
        return "true".equals(mappingModel.getConfig().get(ProtocolMapperUtils.MULTIVALUED));
    }

    public static boolean includeInUserInfo(ProtocolMapperModel mappingModel){
        String includeInUserInfo = mappingModel.getConfig().get(INCLUDE_IN_USERINFO);

        // Backwards compatibility
        if (includeInUserInfo == null && includeInIDToken(mappingModel)) {
            return true;
        }

        return "true".equals(includeInUserInfo);
    }

    public static boolean includeInIntrospection(ProtocolMapperModel mappingModel) {
        String includeInIntrospection = mappingModel.getConfig().get(INCLUDE_IN_INTROSPECTION);

        // Backwards compatibility
        if (includeInIntrospection == null && includeInAccessToken(mappingModel)) {
            return true;
        }

        return "true".equals(includeInIntrospection);
    }

    public static boolean includeInLightweightAccessToken(ProtocolMapperModel mappingModel) {
        return "true".equals(mappingModel.getConfig().get(INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN));
    }

    public static void addAttributeConfig(List<ProviderConfigProperty> configProperties, Class<? extends ProtocolMapper> protocolMapperClass) {
        addTokenClaimNameConfig(configProperties);
        addJsonTypeConfig(configProperties);

        addIncludeInTokensConfig(configProperties, protocolMapperClass);
    }

    public static void addTokenClaimNameConfig(List<ProviderConfigProperty> configProperties) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(TOKEN_CLAIM_NAME);
        property.setLabel(TOKEN_CLAIM_NAME_LABEL);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText(TOKEN_CLAIM_NAME_TOOLTIP);
        property.setRequired(true);
        configProperties.add(property);
    }

    public static void addJsonTypeConfig(List<ProviderConfigProperty> configProperties) {
        addJsonTypeConfig(configProperties, List.of("String", "long", "int", "boolean", "JSON"), null);
    }

    public static void addJsonTypeConfig(List<ProviderConfigProperty> configProperties, List<String> supportedTypes, String defaultValue) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(JSON_TYPE);
        property.setLabel(JSON_TYPE);
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(supportedTypes);
        property.setHelpText(JSON_TYPE_TOOLTIP);
        property.setDefaultValue(defaultValue);
        configProperties.add(property);
    }

    public static void addIncludeInTokensConfig(List<ProviderConfigProperty> configProperties, Class<? extends ProtocolMapper> protocolMapperClass) {
        if (OIDCIDTokenMapper.class.isAssignableFrom(protocolMapperClass)) {
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setName(INCLUDE_IN_ID_TOKEN);
            property.setLabel(INCLUDE_IN_ID_TOKEN_LABEL);
            property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
            property.setDefaultValue("true");
            property.setHelpText(INCLUDE_IN_ID_TOKEN_HELP_TEXT);
            configProperties.add(property);
        }

        if (OIDCAccessTokenMapper.class.isAssignableFrom(protocolMapperClass)) {
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setName(INCLUDE_IN_ACCESS_TOKEN);
            property.setLabel(INCLUDE_IN_ACCESS_TOKEN_LABEL);
            property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
            property.setDefaultValue("true");
            property.setHelpText(INCLUDE_IN_ACCESS_TOKEN_HELP_TEXT);
            configProperties.add(property);

            ProviderConfigProperty property2 = new ProviderConfigProperty();
            property2.setName(INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN);
            property2.setLabel(INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN_LABEL);
            property2.setType(ProviderConfigProperty.BOOLEAN_TYPE);
            property2.setDefaultValue("false");
            property2.setHelpText(INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN_HELP_TEXT);
            configProperties.add(property2);
        }

        if (UserInfoTokenMapper.class.isAssignableFrom(protocolMapperClass)) {
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setName(INCLUDE_IN_USERINFO);
            property.setLabel(INCLUDE_IN_USERINFO_LABEL);
            property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
            property.setDefaultValue("true");
            property.setHelpText(INCLUDE_IN_USERINFO_HELP_TEXT);
            configProperties.add(property);
        }

        if (OIDCAccessTokenResponseMapper.class.isAssignableFrom(protocolMapperClass)) {
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setName(INCLUDE_IN_ACCESS_TOKEN_RESPONSE);
            property.setLabel(INCLUDE_IN_ACCESS_TOKEN_RESPONSE_LABEL);
            property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
            property.setDefaultValue("false");
            property.setHelpText(INCLUDE_IN_ACCESS_TOKEN_RESPONSE_HELP_TEXT);
            configProperties.add(property);
        }

        if (TokenIntrospectionTokenMapper.class.isAssignableFrom(protocolMapperClass)) {
            ProviderConfigProperty property = new ProviderConfigProperty();
            property.setName(INCLUDE_IN_INTROSPECTION);
            property.setLabel(INCLUDE_IN_INTROSPECTION_LABEL);
            property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
            property.setDefaultValue("true");
            property.setHelpText(INCLUDE_IN_INTROSPECTION_HELP_TEXT);
            configProperties.add(property);
        }
    }
}
