/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.client.policies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientRolesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateContextConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureClientAuthenticatorExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureRequestObjectExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureResponseTypeExecutor;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureSigningAlgorithmEnforceExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureSigningAlgorithmForSignedJwtEnforceExecutorConfig;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.ws.rs.BadRequestException;

import org.apache.http.HttpResponse;
import org.hamcrest.Matchers;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.Profile;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.Constants;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AuthorizationResponseToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthenticatorExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureLogoutExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureParContentsExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutor;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureResponseTypeExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSessionEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmForSignedJwtExecutorFactory;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.pages.OAuth2DeviceVerificationPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.testsuite.util.OAuthClient.ParResponse;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * This test class is for testing an executor of client policies.
 * 
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
@EnableFeature(value = Profile.Feature.CLIENT_SECRET_ROTATION)
public class ClientPoliciesExecutorTest extends AbstractClientPoliciesTest {

    
    protected OAuth2DeviceVerificationPage verificationPage;

    
    protected OAuthGrantPage grantPage;

    
    protected ErrorPage errorPage;

    
    protected LogoutConfirmPage logoutConfirmPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        List<UserRepresentation> users = realm.getUsers();

        LinkedList<CredentialRepresentation> credentials = new LinkedList<>();
        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue("password");
        credentials.add(password);

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername("manage-clients");
        user.setCredentials(credentials);
        user.setClientRoles(Collections.singletonMap(Constants.REALM_MANAGEMENT_CLIENT_ID, Collections.singletonList(AdminRoles.MANAGE_CLIENTS)));

        users.add(user);

        user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername("create-clients");
        user.setCredentials(credentials);
        user.setClientRoles(Collections.singletonMap(Constants.REALM_MANAGEMENT_CLIENT_ID, Collections.singletonList(AdminRoles.CREATE_CLIENT)));
        user.setGroups(Arrays.asList("topGroup")); // defined in testrealm.json

        users.add(user);

        realm.setUsers(users);

        List<ClientRepresentation> clients = realm.getClients();

        ClientRepresentation app = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("test-device")
                .secret("secret")
                .attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true")
                .attribute(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+")
                .build();
        clients.add(app);

        ClientRepresentation appPublic = ClientBuilder.create().id(KeycloakModelUtils.generateId()).publicClient()
                .clientId(DEVICE_APP_PUBLIC)
                .attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true")
                .attribute(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+")
                .build();
        clients.add(appPublic);

        userId = KeycloakModelUtils.generateId();
        UserRepresentation deviceUser = UserBuilder.create()
                .id(userId)
                .username("device-login")
                .email("device-login@localhost")
                .password("password")
                .build();
        users.add(deviceUser);

        testRealms.add(realm);
    }

    // Tests that secured client authenticator is enforced also during client authentication itself (during token request after successful login)
    @Test
    public void testSecureClientAuthenticatorDuringLogin() throws Exception {
        // register profile to NOT allow authentication with ClientIdAndSecret
        String profileName = "MyProfile";
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(profileName, "Primum Profile")
                        .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                                createSecureClientAuthenticatorExecutorConfig(
                                        Arrays.asList(JWTClientAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID, X509ClientAuthenticator.PROVIDER_ID),
                                        null))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register role policy
        String roleAlphaName = "sample-client-role-alpha";
        String roleZetaName = "sample-client-role-zeta";
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politikken", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(roleAlphaName, roleZetaName)))
                        .addProfile(profileName)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // create a client without client role. It should be successful (policy not applied)
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cId = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret("secret");
        });

        // Login with clientIdAndSecret. It should be successful (policy not applied)
        successfulLoginAndLogout(clientId, "secret");

        // Add role to the client
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        Assert.assertEquals(ClientIdAndSecretAuthenticator.PROVIDER_ID, clientRep.getClientAuthenticatorType());
        clientResource.roles().create(RoleBuilder.create().name(roleAlphaName).build());

        // Not allowed to client authentication with clientIdAndSecret anymore. Client matches policy now
        oauth.clientId(clientId);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, "secret");
        assertEquals(400, res.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, res.getError());
        assertEquals("Configured client authentication method not allowed for client", res.getErrorDescription());
    }

    @Test
    public void testSecureResponseTypeExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "O Primeiro Perfil")
                        .addExecutor(SecureResponseTypeExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "A Primeira Politica", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            clientRep.setPublicClient(Boolean.FALSE);
        });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        oauth.clientId(clientId);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("invalid response_type", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN);
        oauth.nonce("vbwe566fsfffds");
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        EventRepresentation loginEvent = events.expectLogin().client(clientId).assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = new OAuthClient.AuthorizationEndpointResponse(oauth).getCode();
        OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, res.getStatusCode());
        events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

        oauth.doLogout(res.getRefreshToken(), clientSecret);
        events.expectLogout(sessionId).client(clientId).clearDetails().assertEvent();

        // update profiles
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "O Primeiro Perfil")
                        .addExecutor(SecureResponseTypeExecutorFactory.PROVIDER_ID, createSecureResponseTypeExecutor(Boolean.FALSE, Boolean.TRUE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN + " " + OIDCResponseType.TOKEN); // token response type allowed
        oauth.nonce("cie8cjcwiw");
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        loginEvent = events.expectLogin().client(clientId).assertEvent();
        sessionId = loginEvent.getSessionId();
        codeId = loginEvent.getDetails().get(Details.CODE_ID);
        code = new OAuthClient.AuthorizationEndpointResponse(oauth).getCode();
        res = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, res.getStatusCode());
        events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

        oauth.doLogout(res.getRefreshToken(), clientSecret);
        events.expectLogout(sessionId).client(clientId).clearDetails().assertEvent();

        // shall allow code using response_mode jwt
        oauth.responseType(OIDCResponseType.CODE);
        oauth.responseMode("jwt");
        OAuthClient.AuthorizationEndpointResponse authzResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        String jwsResponse = authzResponse.getResponse();
        AuthorizationResponseToken responseObject = oauth.verifyAuthorizationResponseToken(jwsResponse);
        code = (String) responseObject.getOtherClaims().get(OAuth2Constants.CODE);
        res = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, res.getStatusCode());

        // update profiles
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "O Primeiro Perfil")
                        .addExecutor(SecureResponseTypeExecutorFactory.PROVIDER_ID, createSecureResponseTypeExecutor(Boolean.FALSE, Boolean.FALSE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        oauth.openLogout();
        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN + " " + OIDCResponseType.TOKEN); // token response type allowed
        oauth.responseMode("jwt");
        oauth.openLoginForm();
        final JWSInput errorJws = new JWSInput(new OAuthClient.AuthorizationEndpointResponse(oauth).getResponse());
        JsonNode errorClaims = JsonSerialization.readValue(errorJws.getContent(), JsonNode.class);
        assertEquals(OAuthErrorException.INVALID_REQUEST, errorClaims.get("error").asText());
    }

    @Test
    public void testSecureResponseTypeExecutorAllowTokenResponseType() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "O Primeiro Perfil")
                        .addExecutor(SecureResponseTypeExecutorFactory.PROVIDER_ID, createSecureResponseTypeExecutor(null, Boolean.TRUE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forsta Policyn", Boolean.TRUE)
                        .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                                createClientUpdateContextConditionConfig(Arrays.asList(
                                        ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER,
                                        ClientUpdaterContextConditionFactory.BY_INITIAL_ACCESS_TOKEN,
                                        ClientUpdaterContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)))
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // create by Admin REST API
        try {
            createClientByAdmin(generateSuffixedName("App-by-Admin"), (ClientRepresentation clientRep) -> {
                clientRep.setSecret("secret");
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // update profiles
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "O Primeiro Perfil")
                        .addExecutor(SecureResponseTypeExecutorFactory.PROVIDER_ID, createSecureResponseTypeExecutor(Boolean.TRUE, null))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        String cId = null;
        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        try {
            cId = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setSecret(clientSecret);
                clientRep.setStandardFlowEnabled(Boolean.TRUE);
                clientRep.setImplicitFlowEnabled(Boolean.TRUE);
                clientRep.setPublicClient(Boolean.FALSE);
            });
        } catch (ClientPolicyException e) {
            fail();
        }
        ClientRepresentation cRep = getClientByAdmin(cId);
        assertEquals(Boolean.TRUE.toString(), cRep.getAttributes().get(OIDCConfigAttributes.ID_TOKEN_AS_DETACHED_SIGNATURE));

        adminClient.realm(REALM_NAME).clients().get(cId).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        oauth.clientId(clientId);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("invalid response_type", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN);
        oauth.nonce("LIVieviDie028f");
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        EventRepresentation loginEvent = events.expectLogin().client(clientId).assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = new OAuthClient.AuthorizationEndpointResponse(oauth).getCode();

        IDToken idToken = oauth.verifyIDToken(new OAuthClient.AuthorizationEndpointResponse(oauth).getIdToken());
        // confirm ID token as detached signature does not include authenticated user's claims
        Assert.assertNull(idToken.getEmailVerified());
        Assert.assertNull(idToken.getName());
        Assert.assertNull(idToken.getPreferredUsername());
        Assert.assertNull(idToken.getGivenName());
        Assert.assertNull(idToken.getFamilyName());
        Assert.assertNull(idToken.getEmail());
        assertEquals("LIVieviDie028f", idToken.getNonce());
        // confirm an access token not returned
        Assert.assertNull(new OAuthClient.AuthorizationEndpointResponse(oauth).getAccessToken());

        OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, res.getStatusCode());
        events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

        oauth.doLogout(res.getRefreshToken(), clientSecret);
        events.expectLogout(sessionId).client(clientId).clearDetails().assertEvent();
    }

    @Test
    public void testSecureRequestObjectExecutor() throws Exception {
        Integer availablePeriod = Integer.valueOf(SecureRequestObjectExecutor.DEFAULT_AVAILABLE_PERIOD + 400);
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Prvy Profil")
                        .addExecutor(SecureRequestObjectExecutorFactory.PROVIDER_ID,
                                createSecureRequestObjectExecutorConfig(availablePeriod, null))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Prva Politika", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestUris(Arrays.asList(TestApplicationResourceUrls.clientRequestUri()));
        });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        oauth.clientId(clientId);
        AuthorizationEndpointRequestObject requestObject;

        // check whether whether request object exists
        oauth.request(null);
        oauth.requestUri(null);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Missing parameter: 'request' or 'request_uri'", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // check whether request_uri is https scheme
        // cannot test because existing AuthorizationEndpoint check and return error before executing client policy

        // check whether request object can be retrieved from request_uri
        // cannot test because existing AuthorizationEndpoint check and return error before executing client policy

        // check whether request object can be parsed successfully
        // cannot test because existing AuthorizationEndpoint check and return error before executing client policy

        // check whether scope exists in both query parameter and request object
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setScope(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Invalid parameter. Parameters in 'request' object not matching with request parameters", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // check whether client_id exists in both query parameter and request object
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setClientId(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Invalid parameter. Parameters in 'request' object not matching with request parameters", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // check whether response_type exists in both query parameter and request object
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setResponseType(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Invalid parameter. Parameters in 'request' object not matching with request parameters", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // Check scope required
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setScope(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);
        oauth.scope(null);
        oauth.openid(false);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Parameter 'scope' missing in the request parameters or in 'request' object", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));
        oauth.openid(true);

        // check whether "exp" claim exists
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.exp(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Missing parameter in the 'request' object: exp", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // check whether request object not expired
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.exp(Long.valueOf(0));
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Request Expired", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // check whether "nbf" claim exists
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.nbf(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Missing parameter in the 'request' object: nbf", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // check whether request object not yet being processed
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.nbf(requestObject.getNbf() + 600);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Request not yet being processed", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // check whether request object's available period is short
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.exp(requestObject.getNbf() + availablePeriod.intValue() + 1);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Request's available period is long", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // check whether "aud" claim exists
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.audience((String) null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Missing parameter in the 'request' object: aud", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // check whether "aud" claim points to this keycloak as authz server
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.audience(suiteContext.getAuthServerInfo().getContextRoot().toString());
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST_URI, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Invalid parameter in the 'request' object: aud", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // confirm whether all parameters in query string are included in the request object, and have the same values
        // argument "request" are parameters overridden by parameters in request object
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setState("notmatchstate");
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Invalid parameter. Parameters in 'request' object not matching with request parameters", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // valid request object
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, true);

        successfulLoginAndLogout(clientId, clientSecret);

        // update profile : no configuration - "nbf" check and available period is 3600 sec
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Prvy Profil")
                        .addExecutor(SecureRequestObjectExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // check whether "nbf" claim exists
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.nbf(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Missing parameter in the 'request' object: nbf", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // check whether request object not yet being processed
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.nbf(requestObject.getNbf() + 600);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Request not yet being processed", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // check whether request object's available period is short
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.exp(requestObject.getNbf() + SecureRequestObjectExecutor.DEFAULT_AVAILABLE_PERIOD + 1);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Request's available period is long", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        // update profile : not check "nbf"
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Prvy Profil")
                        .addExecutor(SecureRequestObjectExecutorFactory.PROVIDER_ID,
                                createSecureRequestObjectExecutorConfig(null, Boolean.FALSE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // not check whether "nbf" claim exists
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.nbf(null);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        successfulLoginAndLogout(clientId, clientSecret);

        // not check whether request object not yet being processed
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.nbf(requestObject.getNbf() + 600);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        successfulLoginAndLogout(clientId, clientSecret);

        // not check whether request object's available period is short
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.exp(requestObject.getNbf() + SecureRequestObjectExecutor.DEFAULT_AVAILABLE_PERIOD + 1);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        successfulLoginAndLogout(clientId, clientSecret);

        // update profile : force request object encryption
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Prvy Profil")
                        .addExecutor(SecureRequestObjectExecutorFactory.PROVIDER_ID, createSecureRequestObjectExecutorConfig(null, null, true))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        registerRequestObject(requestObject, clientId, Algorithm.ES256, false);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Request object not encrypted", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));
    }

    @Test
    public void testParSecureRequestObjectExecutor() throws Exception {
        Integer availablePeriod = Integer.valueOf(SecureRequestObjectExecutor.DEFAULT_AVAILABLE_PERIOD + 400);
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Prvy Profil")
                        .addExecutor(SecureRequestObjectExecutorFactory.PROVIDER_ID,
                                createSecureRequestObjectExecutorConfig(availablePeriod, true))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Prva Politika", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestUris(Arrays.asList(TestApplicationResourceUrls.clientRequestUri()));
        });

        oauth.realm(REALM_NAME);
        oauth.clientId(clientId);

        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(SAMPLE_CLIENT_ROLE).build());

        AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);

        oauth.request(signRequestObject(requestObject));
        OAuthClient.ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        oauth.scope(null);
        oauth.responseType(null);
        oauth.request(null);
        oauth.requestUri(requestUri);
        OAuthClient.AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        assertNotNull(loginResponse.getCode());
        oauth.openLogout();

        requestObject.exp(null);
        oauth.requestUri(null);
        oauth.request(signRequestObject(requestObject));
        pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
        requestUri = pResp.getRequestUri();
        oauth.request(null);
        oauth.requestUri(requestUri);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST_URI, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));

        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.nbf(null);
        oauth.requestUri(null);
        oauth.request(signRequestObject(requestObject));
        pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
        requestUri = pResp.getRequestUri();
        oauth.request(null);
        oauth.requestUri(requestUri);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST_URI, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));

        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.audience("https://www.other1.example.com/");
        oauth.request(signRequestObject(requestObject));
        oauth.requestUri(null);
        pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
        requestUri = pResp.getRequestUri();
        oauth.request(null);
        oauth.requestUri(requestUri);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST_URI, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));

        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setOtherClaims(OIDCLoginProtocol.REQUEST_URI_PARAM, "foo");
        oauth.request(signRequestObject(requestObject));
        oauth.requestUri(null);
        pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, pResp.getError());
    }

    @Test
    public void testSecureSessionEnforceExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                        .addExecutor(SecureSessionEnforceExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        String roleAlphaName = "sample-client-role-alpha";
        String roleBetaName = "sample-client-role-beta";
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politikken", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(roleBetaName)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientAlphaId = generateSuffixedName("Alpha-App");
        String clientAlphaSecret = "secretAlpha";
        String cAlphaId = createClientByAdmin(clientAlphaId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientAlphaSecret);
        });
        adminClient.realm(REALM_NAME).clients().get(cAlphaId).roles().create(RoleBuilder.create().name(roleAlphaName).build());

        String clientBetaId = generateSuffixedName("Beta-App");
        String clientBetaSecret = "secretBeta";
        String cBetaId = createClientByAdmin(clientBetaId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientBetaSecret);
        });
        adminClient.realm(REALM_NAME).clients().get(cBetaId).roles().create(RoleBuilder.create().name(roleBetaName).build());

        successfulLoginAndLogout(clientAlphaId, clientAlphaSecret);

        oauth.openid(false);
        successfulLoginAndLogout(clientAlphaId, clientAlphaSecret);

        oauth.openid(true);
        failLoginWithoutSecureSessionParameter(clientBetaId, ERR_MSG_MISSING_NONCE);

        oauth.nonce("yesitisnonce");
        successfulLoginAndLogout(clientBetaId, clientBetaSecret);

        oauth.openid(false);
        oauth.stateParamHardcoded(null);
        failLoginWithoutSecureSessionParameter(clientBetaId, ERR_MSG_MISSING_STATE);

        oauth.stateParamRandom();
        successfulLoginAndLogout(clientBetaId, clientBetaSecret);
    }

    @Test
    public void testSecureSigningAlgorithmEnforceExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forsta Profilen")
                        .addExecutor(SecureSigningAlgorithmExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forsta Policyn", Boolean.TRUE)
                        .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                                createClientUpdateContextConditionConfig(Arrays.asList(
                                        ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER,
                                        ClientUpdaterContextConditionFactory.BY_INITIAL_ACCESS_TOKEN,
                                        ClientUpdaterContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // create by Admin REST API - fail
        try {
            createClientByAdmin(generateSuffixedName("App-by-Admin"), (ClientRepresentation clientRep) -> {
                clientRep.setSecret("secret");
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, "none");
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_REQUEST, e.getMessage());
        }

        // create by Admin REST API - success
        String cAppAdminId = createClientByAdmin(generateSuffixedName("App-by-Admin"), (ClientRepresentation clientRep) -> {
            clientRep.setAttributes(new HashMap<>());
            clientRep.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.PS256);
            clientRep.getAttributes().put(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG, Algorithm.ES256);
            clientRep.getAttributes().put(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.ES256);
            clientRep.getAttributes().put(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, Algorithm.ES256);
            clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.ES256);
        });

        // create by Admin REST API - success, PS256 enforced
        String cAppAdmin2Id = createClientByAdmin(generateSuffixedName("App-by-Admin2"), (ClientRepresentation client2Rep) -> {
        });
        ClientRepresentation cRep2 = getClientByAdmin(cAppAdmin2Id);
        assertEquals(Algorithm.PS256, cRep2.getAttributes().get(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG));
        assertEquals(Algorithm.PS256, cRep2.getAttributes().get(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG));
        assertEquals(Algorithm.PS256, cRep2.getAttributes().get(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG));
        assertEquals(Algorithm.PS256, cRep2.getAttributes().get(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG));
        assertEquals(Algorithm.PS256, cRep2.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));

        // update by Admin REST API - fail
        try {
            updateClientByAdmin(cAppAdminId, (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.RS512);
            });
        } catch (ClientPolicyException cpe) {
            assertEquals(Errors.INVALID_REQUEST, cpe.getError());
        }
        ClientRepresentation cRep = getClientByAdmin(cAppAdminId);
        assertEquals(Algorithm.ES256, cRep.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));

        // update by Admin REST API - success
        updateClientByAdmin(cAppAdminId, (ClientRepresentation clientRep) -> {
            clientRep.setAttributes(new HashMap<>());
            clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, Algorithm.PS384);
        });
        cRep = getClientByAdmin(cAppAdminId);
        assertEquals(Algorithm.PS384, cRep.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));

        // update profiles, ES256 enforced
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forsta Profilen")
                        .addExecutor(SecureSigningAlgorithmExecutorFactory.PROVIDER_ID,
                                createSecureSigningAlgorithmEnforceExecutorConfig(Algorithm.ES256))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // update by Admin REST API - success
        updateClientByAdmin(cAppAdmin2Id, (ClientRepresentation client2Rep) -> {
            client2Rep.getAttributes().remove(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG);
            client2Rep.getAttributes().remove(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG);
            client2Rep.getAttributes().remove(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG);
            client2Rep.getAttributes().remove(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG);
            client2Rep.getAttributes().remove(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG);
        });
        cRep2 = getClientByAdmin(cAppAdmin2Id);
        assertEquals(Algorithm.ES256, cRep2.getAttributes().get(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG));
        assertEquals(Algorithm.ES256, cRep2.getAttributes().get(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG));
        assertEquals(Algorithm.ES256, cRep2.getAttributes().get(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG));
        assertEquals(Algorithm.ES256, cRep2.getAttributes().get(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG));
        assertEquals(Algorithm.ES256, cRep2.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));

        // update profiles, fall back to PS256
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forsta Profilen")
                        .addExecutor(SecureSigningAlgorithmExecutorFactory.PROVIDER_ID,
                                createSecureSigningAlgorithmEnforceExecutorConfig(Algorithm.RS512))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // create dynamically - fail
        try {
            createClientByAdmin(generateSuffixedName("App-in-Dynamic"), (ClientRepresentation clientRep) -> {
                clientRep.setSecret("secret");
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.RS384);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_REQUEST, e.getMessage());
        }

        // create dynamically - success
        String cAppDynamicClientId = createClientDynamically(generateSuffixedName("App-in-Dynamic"), (OIDCClientRepresentation clientRep) -> {
            clientRep.setUserinfoSignedResponseAlg(Algorithm.ES256);
            clientRep.setRequestObjectSigningAlg(Algorithm.ES256);
            clientRep.setIdTokenSignedResponseAlg(Algorithm.PS256);
            clientRep.setTokenEndpointAuthSigningAlg(Algorithm.PS256);
        });
        events.expect(EventType.CLIENT_REGISTER).client(cAppDynamicClientId).user(Matchers.isEmptyOrNullString()).assertEvent();

        // update dynamically - fail
        try {
            updateClientDynamically(cAppDynamicClientId, (OIDCClientRepresentation clientRep) -> {
                clientRep.setIdTokenSignedResponseAlg(Algorithm.RS256);
            });
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }
        assertEquals(Algorithm.PS256, getClientDynamically(cAppDynamicClientId).getIdTokenSignedResponseAlg());

        // update dynamically - success
        updateClientDynamically(cAppDynamicClientId, (OIDCClientRepresentation clientRep) -> {
            clientRep.setIdTokenSignedResponseAlg(Algorithm.ES384);
        });
        assertEquals(Algorithm.ES384, getClientDynamically(cAppDynamicClientId).getIdTokenSignedResponseAlg());

        // create dynamically - success, PS256 enforced
        restartAuthenticatedClientRegistrationSetting();
        String cAppDynamicClient2Id = createClientDynamically(generateSuffixedName("App-in-Dynamic"), (OIDCClientRepresentation client2Rep) -> {
        });
        OIDCClientRepresentation cAppDynamicClient2Rep = getClientDynamically(cAppDynamicClient2Id);
        assertEquals(Algorithm.PS256, cAppDynamicClient2Rep.getUserinfoSignedResponseAlg());
        assertEquals(Algorithm.PS256, cAppDynamicClient2Rep.getRequestObjectSigningAlg());
        assertEquals(Algorithm.PS256, cAppDynamicClient2Rep.getIdTokenSignedResponseAlg());
        assertEquals(Algorithm.PS256, cAppDynamicClient2Rep.getTokenEndpointAuthSigningAlg());

        // update profiles, enforce ES256
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forsta Profilen")
                        .addExecutor(SecureSigningAlgorithmExecutorFactory.PROVIDER_ID,
                                createSecureSigningAlgorithmEnforceExecutorConfig(Algorithm.ES256))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // update dynamically - success, ES256 enforced
        updateClientDynamically(cAppDynamicClient2Id, (OIDCClientRepresentation client2Rep) -> {
            client2Rep.setUserinfoSignedResponseAlg(null);
            client2Rep.setRequestObjectSigningAlg(null);
            client2Rep.setIdTokenSignedResponseAlg(null);
            client2Rep.setTokenEndpointAuthSigningAlg(null);
        });
        cAppDynamicClient2Rep = getClientDynamically(cAppDynamicClient2Id);
        assertEquals(Algorithm.ES256, cAppDynamicClient2Rep.getUserinfoSignedResponseAlg());
        assertEquals(Algorithm.ES256, cAppDynamicClient2Rep.getRequestObjectSigningAlg());
        assertEquals(Algorithm.ES256, cAppDynamicClient2Rep.getIdTokenSignedResponseAlg());
        assertEquals(Algorithm.ES256, cAppDynamicClient2Rep.getTokenEndpointAuthSigningAlg());
    }

    @Test
    public void testSecureClientRegisteringUriEnforceExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Ensimmainen Profiili")
                        .addExecutor(SecureClientUrisExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Ensimmainen Politiikka", Boolean.TRUE)
                        .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                                createClientUpdateContextConditionConfig(Arrays.asList(
                                        ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER,
                                        ClientUpdaterContextConditionFactory.BY_INITIAL_ACCESS_TOKEN,
                                        ClientUpdaterContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        try {
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
                clientRep.setRedirectUris(Collections.singletonList("http://newredirect"));
            });
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }

        String cid = null;
        String clientId = generateSuffixedName(CLIENT_NAME);
        try {
            cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setServiceAccountsEnabled(Boolean.TRUE);
                clientRep.setRedirectUris(null);
            });
        } catch (Exception e) {
            fail();
        }

        updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
            clientRep.setRedirectUris(null);
            clientRep.setServiceAccountsEnabled(Boolean.FALSE);
        });
        assertEquals(false, getClientByAdmin(cid).isServiceAccountsEnabled());

        // update policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Paivitetyn Ensimmaisen Politiikka", Boolean.TRUE)
                        .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                                createClientUpdateContextConditionConfig(Arrays.asList(
                                        ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER,
                                        ClientUpdaterContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        try {
            updateClientDynamically(clientId, (OIDCClientRepresentation clientRep) -> {
                clientRep.setRedirectUris(Collections.singletonList("https://newredirect/*"));
            });
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // rootUrl
                clientRep.setRootUrl("https://client.example.com/");
                // adminUrl
                clientRep.setAdminUrl("https://client.example.com/admin/");
                // baseUrl
                clientRep.setBaseUrl("https://client.example.com/base/");
                // web origins
                clientRep.setWebOrigins(Arrays.asList("https://valid.other.client.example.com/", "https://valid.another.client.example.com/"));
                // backchannel logout URL
                Map<String, String> attributes = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
                attributes.put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, "https://client.example.com/logout/");
                clientRep.setAttributes(attributes);
                // OAuth2 : redirectUris
                clientRep.setRedirectUris(Arrays.asList("https://client.example.com/redirect/", "https://client.example.com/callback/"));
                // OAuth2 : jwks_uri
                attributes.put(OIDCConfigAttributes.JWKS_URL, "https://client.example.com/jwks/");
                clientRep.setAttributes(attributes);
                // OIDD : requestUris
                setAttributeMultivalued(clientRep, OIDCConfigAttributes.REQUEST_URIS, Arrays.asList("https://client.example.com/request/", "https://client.example.com/reqobj/"));
                // CIBA Client Notification Endpoint
                attributes.put(CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT, "https://client.example.com/client-notification/");
                clientRep.setAttributes(attributes);
            });
        } catch (Exception e) {
            fail();
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // rootUrl
                clientRep.setRootUrl("http://client.example.com/*/");
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid rootUrl", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // adminUrl
                clientRep.setAdminUrl("http://client.example.com/admin/");
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid adminUrl", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // baseUrl
                clientRep.setBaseUrl("https://client.example.com/base/*");
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid baseUrl", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // web origins
                clientRep.setWebOrigins(Arrays.asList("http://valid.another.client.example.com/"));
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid webOrigins", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // backchannel logout URL
                Map<String, String> attributes = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
                attributes.put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, "httpss://client.example.com/logout/");
                clientRep.setAttributes(attributes);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid logoutUrl", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // OAuth2 : redirectUris
                clientRep.setRedirectUris(Arrays.asList("https://client.example.com/redirect/", "ftp://client.example.com/callback/"));
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid redirectUris", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // OAuth2 : jwks_uri
                Map<String, String> attributes = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
                attributes.put(OIDCConfigAttributes.JWKS_URL, "http s://client.example.com/jwks/");
                clientRep.setAttributes(attributes);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid jwksUri", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // OIDD : requestUris
                setAttributeMultivalued(clientRep, OIDCConfigAttributes.REQUEST_URIS, Arrays.asList("https://client.example.com/request/*", "https://client.example.com/reqobj/"));
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid requestUris", e.getErrorDetail());
        }

        try {
            updateClientByAdmin(cid, (ClientRepresentation clientRep) -> {
                // CIBA Client Notification Endpoint
                Map<String, String> attributes = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
                attributes.put(CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT, "http://client.example.com/client-notification/");
                clientRep.setAttributes(attributes);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
            assertEquals("Invalid cibaClientNotificationEndpoint", e.getErrorDetail());
        }
    }

    @Test
    public void testSecureSigningAlgorithmForSignedJwtEnforceExecutorWithSecureAlg() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                        (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Ensimmainen Profiili")
                                .addExecutor(SecureSigningAlgorithmForSignedJwtExecutorFactory.PROVIDER_ID, createSecureSigningAlgorithmForSignedJwtEnforceExecutorConfig(Boolean.TRUE)
                                ).toRepresentation()
                )
                .toString();
        updateProfiles(json);

        // register policies
        String roleAlphaName = "sample-client-role-alpha";
        String roleZetaName = "sample-client-role-zeta";
        String roleCommonName = "sample-client-role-common";
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politikken", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(roleAlphaName, roleZetaName)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // create a client with client role
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret("secret");
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            clientRep.setAttributes(new HashMap<>());
            clientRep.getAttributes().put(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, Algorithm.ES256);
        });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(roleAlphaName).build());
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(roleCommonName).build());


        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();

        KeyPair keyPair = setupJwksUrl(Algorithm.ES256, clientRep, clientResource);
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.ES256);

        oauth.clientId(clientId);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        EventRepresentation loginEvent = events.expectLogin()
                .client(clientId)
                .assertEvent();
        String sessionId = loginEvent.getSessionId();
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        // obtain access token
        OAuthClient.AccessTokenResponse response = doAccessTokenRequestWithSignedJWT(code, signedJwt);

        assertEquals(200, response.getStatusCode());
        oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertEquals(sessionId, refreshToken.getSessionState());
        assertEquals(sessionId, refreshToken.getSessionState());
        events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), loginEvent.getSessionId())
                .client(clientId)
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientAuthenticator.PROVIDER_ID)
                .assertEvent();

        // refresh token
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.ES256);
        OAuthClient.AccessTokenResponse refreshedResponse = doRefreshTokenRequestWithSignedJWT(response.getRefreshToken(), signedJwt);
        assertEquals(200, refreshedResponse.getStatusCode());

        // introspect token
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.ES256);
        HttpResponse tokenIntrospectionResponse = doTokenIntrospectionWithSignedJWT("access_token", refreshedResponse.getAccessToken(), signedJwt);
        assertEquals(200, tokenIntrospectionResponse.getStatusLine().getStatusCode());

        // revoke token
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.ES256);
        HttpResponse revokeTokenResponse = doTokenRevokeWithSignedJWT("refresh_toke", refreshedResponse.getRefreshToken(), signedJwt);
        assertEquals(200, revokeTokenResponse.getStatusLine().getStatusCode());

        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.ES256);
        OAuthClient.AccessTokenResponse tokenRes = doRefreshTokenRequestWithSignedJWT(refreshedResponse.getRefreshToken(), signedJwt);
        assertEquals(400, tokenRes.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, tokenRes.getError());

        // logout
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.ES256);
        HttpResponse logoutResponse = doLogoutWithSignedJWT(refreshedResponse.getRefreshToken(), signedJwt);
        assertEquals(204, logoutResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void testSecureSigningAlgorithmForSignedJwtEnforceExecutorWithNotSecureAlg() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Ensimmainen Profiili")
                        .addExecutor(SecureSigningAlgorithmForSignedJwtExecutorFactory.PROVIDER_ID, createSecureSigningAlgorithmForSignedJwtEnforceExecutorConfig(Boolean.FALSE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        String roleAlphaName = "sample-client-role-alpha";
        String roleZetaName = "sample-client-role-zeta";
        String roleCommonName = "sample-client-role-common";
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politikken", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(roleAlphaName, roleZetaName)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // create a client with client role
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret("secret");
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            clientRep.setAttributes(new HashMap<>());
            clientRep.getAttributes().put(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, Algorithm.RS256);
        });
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(roleAlphaName).build());
        adminClient.realm(REALM_NAME).clients().get(cid).roles().create(RoleBuilder.create().name(roleCommonName).build());

        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();

        KeyPair keyPair = setupJwksUrl(Algorithm.RS256, clientRep, clientResource);
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.RS256);

        oauth.clientId(clientId);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        EventRepresentation loginEvent = events.expectLogin()
                .client(clientId)
                .assertEvent();
        String sessionId = loginEvent.getSessionId();
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        // obtain access token
        OAuthClient.AccessTokenResponse response = doAccessTokenRequestWithSignedJWT(code, signedJwt);

        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("not allowed signature algorithm.", response.getErrorDescription());
    }

    @Test
    public void testSecureLogoutExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Logout Test")
                        .addExecutor(SecureLogoutExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Logout Policy", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientId = generateSuffixedName(CLIENT_NAME);
        String clientSecret = "secret";
        try {
            createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
                clientRep.setSecret(clientSecret);
                clientRep.setStandardFlowEnabled(Boolean.TRUE);
                clientRep.setImplicitFlowEnabled(Boolean.TRUE);
                clientRep.setPublicClient(Boolean.FALSE);
                clientRep.setFrontchannelLogout(true);
            });
        } catch (ClientPolicyException cpe) {
            assertEquals("Front-channel logout is not allowed for this client", cpe.getErrorDetail());
        }

        String cid = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            clientRep.setPublicClient(Boolean.FALSE);
        });

        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cid);
        ClientRepresentation clientRep = clientResource.toRepresentation();

        clientRep.setFrontchannelLogout(true);

        try {
            clientResource.update(clientRep);
        } catch (BadRequestException bre) {
            assertEquals("Front-channel logout is not allowed for this client", bre.getResponse().readEntity(OAuth2ErrorRepresentation.class).getErrorDescription());
        }

        ClientPolicyExecutorConfigurationRepresentation config = new ClientPolicyExecutorConfigurationRepresentation();

        config.setConfigAsMap(SecureLogoutExecutorFactory.ALLOW_FRONT_CHANNEL_LOGOUT, Boolean.TRUE.booleanValue());

        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Logout Test")
                        .addExecutor(SecureLogoutExecutorFactory.PROVIDER_ID, config)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setFrontChannelLogoutUrl(oauth.getRedirectUri());
        clientResource.update(clientRep);

        config.setConfigAsMap(SecureLogoutExecutorFactory.ALLOW_FRONT_CHANNEL_LOGOUT, Boolean.FALSE.toString());

        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Logout Test")
                        .addExecutor(SecureLogoutExecutorFactory.PROVIDER_ID, config)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        OAuthClient.AccessTokenResponse response = successfulLogin(clientId, clientSecret);

        oauth.idTokenHint(response.getIdToken()).openLogout();

        assertTrue(driver.getPageSource().contains("Front-channel logout is not allowed for this client"));
    }

    @Test
    public void testSecureParContentsExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureParContentsExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        String clientBetaId = generateSuffixedName("Beta-App");
        createClientByAdmin(clientBetaId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret("secretBeta");
        });

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Pushed Authorization Request
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientBetaId, "secretBeta");
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        oauth.requestUri(requestUri);
        oauth.clientId(clientBetaId);
        oauth.openLoginForm();
        assertTrue(errorPage.isCurrent());
        assertEquals("PAR request did not include necessary parameters", errorPage.getError());

        oauth.requestUri(null);
        pResp = oauth.doPushedAuthorizationRequest(clientBetaId, "secretBeta");
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();
        oauth.requestUri(requestUri);

        oauth.stateParamHardcoded(null);
        successfulLoginAndLogout(clientBetaId, "secretBeta");
    }

    @Test
    public void testSecureParContentsExecutorWithRequestObject() throws Exception {
        // Set up a request object
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setOIDCRequest(REALM_NAME, TEST_CLIENT, oauth.getRedirectUri(), "10", null, "none");
        String encodedRequestObject = oidcClientEndpointsResource.getOIDCRequest();

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureParContentsExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Pushed Authorization Request without state parameter
        oauth.addCustomParameter("request", encodedRequestObject);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(TEST_CLIENT, TEST_CLIENT_SECRET);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // only query parameters include state parameter
        oauth.removeCustomParameter("request");
        oauth.stateParamHardcoded("mystate2");
        oauth.requestUri(requestUri);
        oauth.openLoginForm();
        assertTrue(errorPage.isCurrent());
        assertEquals("PAR request did not include necessary parameters", errorPage.getError());

        // Pushed Authorization Request with state parameter
        oidcClientEndpointsResource.setOIDCRequest(REALM_NAME, TEST_CLIENT, oauth.getRedirectUri(), "10", "mystate2", "none");
        encodedRequestObject = oidcClientEndpointsResource.getOIDCRequest();

        oauth.requestUri(null);
        oauth.addCustomParameter("request", encodedRequestObject);
        pResp = oauth.doPushedAuthorizationRequest(TEST_CLIENT, TEST_CLIENT_SECRET);
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();

        // both query parameters and PAR requests include state parameter
        oauth.removeCustomParameter("request");
        oauth.requestUri(requestUri);
        successfulLoginAndLogout(TEST_CLIENT, TEST_CLIENT_SECRET);

    }
}
