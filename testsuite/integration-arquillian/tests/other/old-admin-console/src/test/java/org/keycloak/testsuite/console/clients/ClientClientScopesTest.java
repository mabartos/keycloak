/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.console.clients;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.console.page.clients.clientscopes.ClientScopesEvaluate;
import org.keycloak.testsuite.console.page.clients.clientscopes.ClientScopesEvaluateForm;
import org.keycloak.testsuite.console.page.clients.clientscopes.ClientScopesSetup;
import org.keycloak.testsuite.console.page.clients.clientscopes.ClientScopesSetupForm;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.hamcrest.MatcherAssert;
import static org.hamcrest.Matchers.hasItems;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.keycloak.testsuite.auth.page.login.Login.OIDC;

/**
 * Test for the "Client Scopes" tab of client (Binding between "Client" and "Client Scopes")
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientClientScopesTest extends AbstractClientTest {

    private ClientRepresentation newClient;
    private ClientRepresentation found;


    @Page
    private ClientScopesSetup clientScopesSetupPage;

    @Page
    private ClientScopesEvaluate clientScopesEvaluatePage;


    @BeforeEach
    public void before() {
        newClient = createClientRep(TEST_CLIENT_ID, OIDC);
        newClient.setFullScopeAllowed(false);

        testRealmResource().clients().create(newClient).close();

        found = findClientByClientId(TEST_CLIENT_ID);
        assertNotNull("Client " + TEST_CLIENT_ID + " was not found.", found);
        clientScopesSetupPage.setId(found.getId());
        clientScopesSetupPage.navigateTo();
    }


    @Test
    public void testSetupClientScopes() {
        ClientScopesSetupForm setupForm = clientScopesSetupPage.form();

        // Test the initial state
        Assertions.assertNames(setupForm.getAvailableDefaultClientScopes());
        Assertions.assertNames(setupForm.getDefaultClientScopes(), "email", "profile", "roles", "web-origins", "acr");
        Assertions.assertNames(setupForm.getAvailableOptionalClientScopes());
        Assertions.assertNames(setupForm.getOptionalClientScopes(), "address", "phone", "offline_access", "microprofile-jwt");

        // Remove 'profile' as default client scope and assert
        setupForm.setDefaultClientScopes(Collections.singletonList("email"));
        Assertions.assertNames(setupForm.getAvailableDefaultClientScopes(), "profile", "roles", "web-origins", "acr");
        Assertions.assertNames(setupForm.getDefaultClientScopes(), "email");
        Assertions.assertNames(setupForm.getAvailableOptionalClientScopes(), "profile", "roles", "web-origins", "acr");
        Assertions.assertNames(setupForm.getOptionalClientScopes(), "address", "phone", "offline_access", "microprofile-jwt");

        // Add 'profile' as optional client scope and assert
        setupForm.setOptionalClientScopes(Arrays.asList("profile", "address", "phone", "offline_access", "microprofile-jwt", "acr"));
        Assertions.assertNames(setupForm.getAvailableDefaultClientScopes(), "roles", "web-origins");
        Assertions.assertNames(setupForm.getDefaultClientScopes(), "email");
        Assertions.assertNames(setupForm.getAvailableOptionalClientScopes(), "roles", "web-origins");
        Assertions.assertNames(setupForm.getOptionalClientScopes(), "profile", "address", "phone", "offline_access", "microprofile-jwt", "acr");

        // Retrieve client through adminClient
        found = findClientByClientId(TEST_CLIENT_ID);
        Assertions.assertNames(found.getDefaultClientScopes(), "email");
        Assertions.assertNames(found.getOptionalClientScopes(), "profile", "address", "phone", "offline_access", "microprofile-jwt", "acr");


        // Revert and check things successfully reverted
        setupForm.setOptionalClientScopes(Arrays.asList("address", "phone", "offline_access", "microprofile-jwt", "acr"));
        Assertions.assertNames(setupForm.getAvailableDefaultClientScopes(), "profile", "roles", "web-origins");
        setupForm.setDefaultClientScopes(Arrays.asList("profile", "email"));

        Assertions.assertNames(setupForm.getAvailableDefaultClientScopes(), "roles", "web-origins");
        Assertions.assertNames(setupForm.getDefaultClientScopes(), "email", "profile");
        Assertions.assertNames(setupForm.getAvailableOptionalClientScopes(), "roles", "web-origins");
        Assertions.assertNames(setupForm.getOptionalClientScopes(), "address", "phone", "offline_access", "microprofile-jwt", "acr");
    }


    @Test
    public void testEvaluateClientScopes() throws IOException {
        clientScopesEvaluatePage.setId(found.getId());
        clientScopesEvaluatePage.navigateTo();

        ClientScopesEvaluateForm evaluateForm = clientScopesEvaluatePage.form();

        // Check the defaults
        Assertions.assertNames(evaluateForm.getAvailableClientScopes(), "address", "phone", "offline_access", "microprofile-jwt");
        Assertions.assertNames(evaluateForm.getAssignedClientScopes());
        Assertions.assertNames(evaluateForm.getEffectiveClientScopes(), "profile", "email", "roles", "web-origins", "acr");

        // Add some optional scopes to the evaluation
        evaluateForm.setAssignedClientScopes(Arrays.asList("address", "phone"));
        Assertions.assertNames(evaluateForm.getAvailableClientScopes(), "offline_access", "microprofile-jwt");
        Assertions.assertNames(evaluateForm.getAssignedClientScopes(), "address", "phone");
        Assertions.assertNames(evaluateForm.getEffectiveClientScopes(), "address", "phone", "profile", "email", "roles", "web-origins", "acr");

        // Remove optional 'phone' scope from the evaluation
        evaluateForm.setAssignedClientScopes(Arrays.asList("address", "offline_access"));
        Assertions.assertNames(evaluateForm.getAvailableClientScopes(), "phone", "microprofile-jwt");
        Assertions.assertNames(evaluateForm.getAssignedClientScopes(), "address", "offline_access");
        Assertions.assertNames(evaluateForm.getEffectiveClientScopes(), "address", "offline_access", "profile", "email", "roles", "web-origins", "acr");

        // Select some user
        evaluateForm.selectUser("test");

        // Submit
        evaluateForm.evaluate();

        // Test protocolMappers of 'address' , 'profile' and 'email' scopes are included
        Set<String> protocolMappers = evaluateForm.getEffectiveProtocolMapperNames();
        Assertions.assertTrue(protocolMappers.contains("address"));
        Assertions.assertTrue(protocolMappers.contains("email"));
        Assertions.assertTrue(protocolMappers.contains("email verified"));
        Assertions.assertTrue(protocolMappers.contains("username"));
        Assertions.assertTrue(protocolMappers.contains("full name"));
        Assertions.assertFalse(protocolMappers.contains("phone"));

        // Test roles
        evaluateForm.showRoles();
        Assertions.assertNames(evaluateForm.getGrantedRealmRoles(), "offline_access");
        MatcherAssert.assertThat(evaluateForm.getNotGrantedRealmRoles(),
                hasItems("uma_authorization", "default-roles-test"));

        // Test access token
        evaluateForm.showToken();
        String accessTokenStr = evaluateForm.getAccessToken();

        AccessToken token = JsonSerialization.readValue(accessTokenStr, AccessToken.class);
        String scopeParam = token.getScope();
        Assertions.assertTrue(TokenUtil.isOIDCRequest(scopeParam));
        Assertions.assertTrue(TokenUtil.hasScope(scopeParam, "address"));
        Assertions.assertTrue(TokenUtil.hasScope(scopeParam, "profile"));
        Assertions.assertTrue(TokenUtil.hasScope(scopeParam, "email"));
        Assertions.assertFalse(TokenUtil.hasScope(scopeParam, "phone"));
    }
}
