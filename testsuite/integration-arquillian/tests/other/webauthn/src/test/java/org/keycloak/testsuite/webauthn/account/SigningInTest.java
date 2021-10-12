/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.webauthn.account;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnPasswordlessAuthenticatorFactory;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.SigningInPage;
import org.keycloak.testsuite.ui.account2.page.utils.SigningInPageUtils;
import org.keycloak.testsuite.webauthn.authenticators.UseVirtualAuthenticators;
import org.keycloak.testsuite.webauthn.pages.WebAuthnRegisterPage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;
import static org.keycloak.testsuite.ui.account2.page.utils.SigningInPageUtils.assertUserCredential;
import static org.keycloak.testsuite.ui.account2.page.utils.SigningInPageUtils.testSetUpLink;
import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

public class SigningInTest extends AbstractWebAuthnAccountTest implements UseVirtualAuthenticators {

    private static final String WEBAUTHN_FLOW_ID = "75e2390e-f296-49e6-acf8-6d21071d7e10";

    @Page
    private SigningInPage signingInPage;

    @Page
    private WebAuthnRegisterPage webAuthnRegisterPage;

    private SigningInPage.CredentialType webAuthnCredentialType;
    private SigningInPage.CredentialType webAuthnPwdlessCredentialType;

    @Override
    protected AbstractLoggedInPage getAccountPage() {
        return signingInPage;
    }

    @Before
    public void beforeSigningInTest() {
        webAuthnCredentialType = signingInPage.getCredentialType(WebAuthnCredentialModel.TYPE_TWOFACTOR);
        webAuthnPwdlessCredentialType = signingInPage.getCredentialType(WebAuthnCredentialModel.TYPE_PASSWORDLESS);
    }

    @Override
    protected void afterAbstractKeycloakTestRealmImport() {
        super.afterAbstractKeycloakTestRealmImport();

        // configure WebAuthn
        // we can't do this during the realm import because we'd need to specify all built-in flows as well

        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setId(WEBAUTHN_FLOW_ID);
        flow.setAlias("webauthn flow");
        flow.setProviderId("basic-flow");
        flow.setBuiltIn(false);
        flow.setTopLevel(true);
        testRealmResource().flows().createFlow(flow);

        AuthenticationExecutionRepresentation execution = new AuthenticationExecutionRepresentation();
        execution.setAuthenticator(WebAuthnAuthenticatorFactory.PROVIDER_ID);
        execution.setPriority(10);
        execution.setRequirement(REQUIRED.toString());
        execution.setParentFlow(WEBAUTHN_FLOW_ID);
        testRealmResource().flows().addExecution(execution);

        execution.setAuthenticator(WebAuthnPasswordlessAuthenticatorFactory.PROVIDER_ID);
        testRealmResource().flows().addExecution(execution);

        RequiredActionProviderSimpleRepresentation requiredAction = new RequiredActionProviderSimpleRepresentation();
        requiredAction.setProviderId(WebAuthnRegisterFactory.PROVIDER_ID);
        requiredAction.setName("blahblah");
        testRealmResource().flows().registerRequiredAction(requiredAction);

        requiredAction.setProviderId(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);
        testRealmResource().flows().registerRequiredAction(requiredAction);

        // no need to actually configure the authentication, in Account Console tests we just verify the registration
    }

    @Test
    public void categoriesTest() {
        testContext.setTestRealmReps(emptyList()); // reimport realm after this test

        assertThat(signingInPage.getCategoriesCount(), is(3));
        assertThat(signingInPage.getCategoryTitle("basic-authentication"), is("Basic Authentication"));
        assertThat(signingInPage.getCategoryTitle("two-factor"), is("Two-Factor Authentication"));
        assertThat(signingInPage.getCategoryTitle("passwordless"), is("Passwordless"));

        // Delete WebAuthn flow ==> Passwordless category should disappear
        testRealmResource().flows().deleteFlow(WEBAUTHN_FLOW_ID);
        refreshPageAndWaitForLoad();

        assertThat(signingInPage.getCategoriesCount(), is(2));
    }

    @Test
    public void twoFactorWebAuthnTest() {
        testWebAuthn(false);
    }

    @Test
    public void passwordlessWebAuthnTest() {
        testWebAuthn(true);
    }

    @Test
    public void testCreateWebAuthnSameUserLabel() {
        final String SAME_LABEL = "key123";

        // Do we really allow to have several authenticators with the same user label??

        SigningInPage.UserCredential webAuthn = addWebAuthnCredential(SAME_LABEL, false);
        assertThat(webAuthn, notNullValue());
        SigningInPage.UserCredential passwordless = addWebAuthnCredential(SAME_LABEL, true);
        assertThat(passwordless, notNullValue());

        assertThat(webAuthnCredentialType.getUserCredentialsCount(), is(1));
        webAuthn = webAuthnCredentialType.getUserCredential(webAuthn.getId());
        assertThat(webAuthn, notNullValue());
        assertThat(webAuthn.getUserLabel(), is(SAME_LABEL));

        assertThat(webAuthnPwdlessCredentialType.getUserCredentialsCount(), is(1));
        passwordless = webAuthnPwdlessCredentialType.getUserCredential(passwordless.getId());
        assertThat(passwordless, notNullValue());
        assertThat(passwordless.getUserLabel(), is(SAME_LABEL));

        SigningInPage.UserCredential webAuthn2 = addWebAuthnCredential(SAME_LABEL, false);
        assertThat(webAuthn2, notNullValue());
        assertThat(webAuthn2.getUserLabel(), is(SAME_LABEL));

        assertThat(webAuthnCredentialType.getUserCredentialsCount(), is(2));

        SigningInPage.UserCredential passwordless2 = addWebAuthnCredential(SAME_LABEL, true);
        assertThat(passwordless2, notNullValue());
        assertThat(passwordless2.getUserLabel(), is(SAME_LABEL));

        assertThat(webAuthnPwdlessCredentialType.getUserCredentialsCount(), is(2));
    }

    @Test
    public void testMultipleSecurityKeys() {
        final String LABEL = "SecurityKey#";

        List<SigningInPage.UserCredential> createdCredentials = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            SigningInPage.UserCredential key = addWebAuthnCredential(LABEL + i, i % 2 == 0);
            assertThat(key, notNullValue());
            createdCredentials.add(key);
        }

        final int webAuthnCount = webAuthnCredentialType.getUserCredentialsCount();
        assertThat(webAuthnCount, is(5));

        final int passwordlessCount = webAuthnPwdlessCredentialType.getUserCredentialsCount();
        assertThat(passwordlessCount, is(5));

        UserResource userResource = testRealmResource().users().get(testUser.getId());
        assertThat(userResource, notNullValue());

        List<CredentialRepresentation> list = userResource.credentials();
        assertThat(list, notNullValue());
        assertThat(list, not(empty()));

        final List<String> credentialsLabels = list.stream()
                .map(CredentialRepresentation::getUserLabel)
                .collect(Collectors.toList());

        assertThat(credentialsLabels, notNullValue());
        final List<String> createdCredentialsLabels = createdCredentials.stream()
                .map(SigningInPage.UserCredential::getUserLabel)
                .collect(Collectors.toList());

        assertThat(credentialsLabels.containsAll(createdCredentialsLabels), is(true));

        final List<SigningInPage.UserCredential> credentials = createdCredentials.stream()
                .filter(key -> key.getUserLabel().equals(LABEL + 0)
                        || key.getUserLabel().equals(LABEL + 1))
                .collect(Collectors.toList());

        assertThat(credentials, hasSize(2));

        testRemoveCredential(credentials.get(0));
        testRemoveCredential(credentials.get(1));

        assertThat(webAuthnCredentialType.getUserCredentialsCount(), is(4));
        assertThat(webAuthnPwdlessCredentialType.getUserCredentialsCount(), is(4));
    }

    @Test
    public void setUpLinksTest() {
        testSetUpLink(testRealmResource(), webAuthnCredentialType, WebAuthnRegisterFactory.PROVIDER_ID);
        testSetUpLink(testRealmResource(), webAuthnPwdlessCredentialType, WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);
    }

    private void testWebAuthn(boolean passwordless) {
        testContext.setTestRealmReps(emptyList());

        SigningInPage.CredentialType credentialType;
        final String expectedHelpText;
        final String providerId;

        if (passwordless) {
            credentialType = webAuthnPwdlessCredentialType;
            expectedHelpText = "Use your security key for passwordless sign in.";
            providerId = WebAuthnPasswordlessRegisterFactory.PROVIDER_ID;
        } else {
            credentialType = webAuthnCredentialType;
            expectedHelpText = "Use your security key to sign in.";
            providerId = WebAuthnRegisterFactory.PROVIDER_ID;
        }

        assertThat(credentialType.isSetUp(), is(false));
        // no way to simulate registration cancellation

        assertThat("Set up link for \"" + credentialType.getType() + "\" is not visible", credentialType.isSetUpLinkVisible(), is(true));
        assertThat(credentialType.getTitle(), is("Security Key"));
        assertThat(credentialType.getHelpText(), is(expectedHelpText));

        final String label1 = "WebAuthn is convenient";
        final String label2 = "but not yet widely adopted";

        SigningInPage.UserCredential webAuthn1 = addWebAuthnCredential(label1, passwordless);
        assertThat(credentialType.isSetUp(), is(true));
        assertThat(credentialType.getUserCredentialsCount(), is(1));
        assertUserCredential(label1, true, webAuthn1);

        SigningInPage.UserCredential webAuthn2 = addWebAuthnCredential(label2, passwordless);
        assertThat(credentialType.getUserCredentialsCount(), is(2));
        assertUserCredential(label2, true, webAuthn2);

        RequiredActionProviderRepresentation requiredAction = new RequiredActionProviderRepresentation();
        requiredAction.setEnabled(false);
        testRealmResource().flows().updateRequiredAction(providerId, requiredAction);

        refreshPageAndWaitForLoad();

        assertThat("Set up link for \"" + credentialType.getType() + "\" is visible", credentialType.isSetUpLinkVisible(), is(false));
        assertThat("Not set up link for \"" + credentialType.getType() + "\" is visible", credentialType.isNotSetUpLabelVisible(), is(false));
        assertThat("Title for \"" + credentialType.getType() + "\" is not visible", credentialType.isTitleVisible(), is(true));
        assertThat(credentialType.getUserCredentialsCount(), is(2));

        testRemoveCredential(webAuthn1);
    }

    private SigningInPage.UserCredential addWebAuthnCredential(String label, boolean passwordless) {
        SigningInPage.CredentialType credentialType = passwordless ? webAuthnPwdlessCredentialType : webAuthnCredentialType;

        credentialType.clickSetUpLink();
        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();
        webAuthnRegisterPage.registerWebAuthnCredential(label);
        waitForPageToLoad();
        signingInPage.assertCurrent();

        return getNewestUserCredential(credentialType);
    }

    private void testRemoveCredential(SigningInPage.UserCredential userCredential) {
        SigningInPageUtils.testRemoveCredential(signingInPage, userCredential);
    }

    private SigningInPage.UserCredential getNewestUserCredential(SigningInPage.CredentialType credentialType) {
        return SigningInPageUtils.getNewestUserCredential(testUserResource(), credentialType);
    }
}
