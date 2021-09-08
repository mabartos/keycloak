/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.webauthn;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.WebAuthnConstants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.keycloak.testsuite.webauthn.authenticators.VirtualAuthenticatorsManager;
import org.keycloak.testsuite.webauthn.pages.WebAuthnLoginPage;
import org.keycloak.testsuite.webauthn.pages.WebAuthnRegisterPage;
import org.keycloak.testsuite.webauthn.updaters.WebAuthnRealmAttributeUpdater;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@EnableFeature(value = Profile.Feature.WEB_AUTHN, skipRestart = true, onlyForProduct = true)
public class WebAuthnRegisterAndLoginTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected WebAuthnLoginPage webAuthnLoginPage;

    @Page
    protected RegisterPage registerPage;

    @Page
    protected WebAuthnRegisterPage webAuthnRegisterPage;

    private static final String ALL_ZERO_AAGUID = "00000000-0000-0000-0000-000000000000";

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void setUpVirtualAuthenticator() {
        VirtualAuthenticatorsManager.getInstance(driver)
                .useAuthenticator(DefaultVirtualAuthOptions.DEFAULT);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/webauthn/testrealm-webauthn.json"), RealmRepresentation.class);
        testRealms.add(realmRepresentation);
    }

    @Test
    public void registerUserSuccess() throws IOException {
        String username = "registerUserSuccess";
        String password = "password";
        String email = "registerUserSuccess@email";

        try (RealmAttributeUpdater rau = updateRealmWithDefaultWebAuthnSettings(testRealm()).update()) {

            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            String authenticatorLabel = SecretGenerator.getInstance().randomString(24);
            registerPage.register("firstName", "lastName", email, username, password, password);

            // User was registered. Now he needs to register WebAuthn credential
            webAuthnRegisterPage.clickRegister();
            webAuthnRegisterPage.registerWebAuthnCredential(authenticatorLabel);

            appPage.assertCurrent();
            assertThat(appPage.getRequestType(), is(RequestType.AUTH_RESPONSE));
            appPage.openAccount();

            // confirm that registration is successfully completed
            String userId = events.expectRegister(username, email).assertEvent().getUserId();
            // confirm registration event
            EventRepresentation eventRep = events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION)
                    .user(userId)
                    .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                    .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, authenticatorLabel)
                    .detail(WebAuthnConstants.PUBKEY_CRED_AAGUID_ATTR, ALL_ZERO_AAGUID)
                    .assertEvent();
            String regPubKeyCredentialId = eventRep.getDetails().get(WebAuthnConstants.PUBKEY_CRED_ID_ATTR);

            // confirm login event
            String sessionId = events.expectLogin()
                    .user(userId)
                    .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                    .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, authenticatorLabel)
                    .assertEvent().getSessionId();
            // confirm user registered
            assertUserRegistered(userId, username.toLowerCase(), email.toLowerCase());
            assertRegisteredCredentials(userId, ALL_ZERO_AAGUID, "none");

            events.clear();

            // logout by user
            appPage.logout();
            // confirm logout event
            events.expectLogout(sessionId)
                    .user(userId)
                    .assertEvent();

            // login by user
            loginPage.open();
            loginPage.login(username, password);

            webAuthnLoginPage.assertCurrent();
            webAuthnLoginPage.clickAuthenticate();

            appPage.assertCurrent();
            assertThat(appPage.getRequestType(), is(RequestType.AUTH_RESPONSE));
            appPage.openAccount();
            // confirm login event
            sessionId = events.expectLogin()
                    .user(userId)
                    .detail(WebAuthnConstants.PUBKEY_CRED_ID_ATTR, regPubKeyCredentialId)
                    .detail("web_authn_authenticator_user_verification_checked", Boolean.FALSE.toString())
                    .assertEvent().getSessionId();

            events.clear();
            // logout by user
            appPage.logout();
            // confirm logout event
            events.expectLogout(sessionId)
                    .user(userId)
                    .assertEvent();
        }
    }

    @Test
    public void testWebAuthnTwoFactorAndWebAuthnPasswordlessTogether() {
        // Change binding to browser-webauthn-passwordless. This is flow, which contains both "webauthn" and "webauthn-passwordless" authenticator
        RealmRepresentation realmRep = testRealm().toRepresentation();
        realmRep.setBrowserFlow("browser-webauthn-passwordless");
        testRealm().update(realmRep);

        try {
            String userId = ApiUtil.findUserByUsername(testRealm(), "test-user@localhost").getId();

            // Login as test-user@localhost with password
            loginPage.open();
            loginPage.login("test-user@localhost", "password");

            // Register first requiredAction is needed. Use label "Label1"
            webAuthnRegisterPage.assertCurrent();
            webAuthnRegisterPage.clickRegister();
            webAuthnRegisterPage.registerWebAuthnCredential("label1");

            // Register second requiredAction is needed. Use label "Label2". This will be for passwordless WebAuthn credential
            webAuthnRegisterPage.assertCurrent();
            webAuthnRegisterPage.clickRegister();
            webAuthnRegisterPage.registerWebAuthnCredential("label2");

            appPage.assertCurrent();

            // Assert user is logged and WebAuthn credentials were registered
            EventRepresentation eventRep = events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION)
                    .user(userId)
                    .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                    .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, "label1")
                    .assertEvent();
            String regPubKeyCredentialId1 = eventRep.getDetails().get(WebAuthnConstants.PUBKEY_CRED_ID_ATTR);
            System.out.println(regPubKeyCredentialId1);

            eventRep = events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION)
                    .user(userId)
                    .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnPasswordlessRegisterFactory.PROVIDER_ID)
                    .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, "label2")
                    .assertEvent();
            String regPubKeyCredentialId2 = eventRep.getDetails().get(WebAuthnConstants.PUBKEY_CRED_ID_ATTR);

            String sessionId = events.expectLogin()
                    .user(userId)
                    .assertEvent().getSessionId();

            events.clear();
            // Logout
            appPage.logout();
            events.expectLogout(sessionId)
                    .user(userId)
                    .assertEvent();

            // Assert user has 2 webauthn credentials. One of type "webauthn" and the other of type "webauthn-passwordless".
            List<CredentialRepresentation> rep = testRealm().users().get(userId).credentials();

            CredentialRepresentation webAuthnCredential1 = rep.stream()
                    .filter(credential -> WebAuthnCredentialModel.TYPE_TWOFACTOR.equals(credential.getType()))
                    .findFirst().orElse(null);

            Assert.assertNotNull(webAuthnCredential1);
            Assert.assertEquals("label1", webAuthnCredential1.getUserLabel());

            CredentialRepresentation webAuthnCredential2 = rep.stream()
                    .filter(credential -> WebAuthnCredentialModel.TYPE_PASSWORDLESS.equals(credential.getType()))
                    .findFirst().orElse(null);

            Assert.assertNotNull(webAuthnCredential2);
            Assert.assertEquals("label2", webAuthnCredential2.getUserLabel());

            events.clear();

            // Assert user needs to authenticate first with "webauthn" during login
            loginPage.open();
            loginPage.login("test-user@localhost", "password");

            // First authenticator
            webAuthnLoginPage.assertCurrent();
            webAuthnLoginPage.clickAuthenticate();

            // Second authenticator
            webAuthnLoginPage.assertCurrent();
            webAuthnLoginPage.clickAuthenticate();

            // Assert user logged now
            appPage.assertCurrent();
            assertThat(appPage.getRequestType(), is(RequestType.AUTH_RESPONSE));
            events.expectLogin()
                    .user(userId)
                    .assertEvent();

            // Remove webauthn credentials from the user
            testRealm().users().get(userId).removeCredential(webAuthnCredential1.getId());
            testRealm().users().get(userId).removeCredential(webAuthnCredential2.getId());
        } finally {
            // Revert binding to browser-webauthn
            realmRep.setBrowserFlow("browser-webauthn");
            testRealm().update(realmRep);
        }
    }

    private void assertUserRegistered(String userId, String username, String email) {
        UserRepresentation user = getUser(userId);
        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getCreatedTimestamp());
        // test that timestamp is current with 60s tollerance
        Assert.assertTrue((System.currentTimeMillis() - user.getCreatedTimestamp()) < 60000);
        // test user info is set from form

        assertThat(user.getUsername(), is(username.toLowerCase()));
        assertThat(user.getEmail(), is(email.toLowerCase()));
        assertThat(user.getFirstName(), is("firstName"));
        assertThat(user.getLastName(), is("lastName"));
    }

    private void assertRegisteredCredentials(String userId, String aaguid, String attestationStatementFormat) {
        List<CredentialRepresentation> credentials = getCredentials(userId);
        credentials.forEach(i -> {
            if (WebAuthnCredentialModel.TYPE_TWOFACTOR.equals(i.getType())) {
                try {
                    WebAuthnCredentialData data = JsonSerialization.readValue(i.getCredentialData(), WebAuthnCredentialData.class);
                    assertThat(data.getAaguid(), is(aaguid));
                    assertThat(data.getAttestationStatementFormat(), is(attestationStatementFormat));
                } catch (IOException e) {
                    Assert.fail();
                }
            }
        });
    }

    protected UserRepresentation getUser(String userId) {
        return testRealm().users().get(userId).toRepresentation();
    }

    protected List<CredentialRepresentation> getCredentials(String userId) {
        return testRealm().users().get(userId).credentials();
    }

    private static WebAuthnRealmAttributeUpdater updateRealmWithDefaultWebAuthnSettings(RealmResource resource) {
        return new WebAuthnRealmAttributeUpdater(resource)
                .setWebAuthnPolicySignatureAlgorithms(Collections.singletonList("ES256"))
                .setWebAuthnPolicyAttestationConveyancePreference("none")
                .setWebAuthnPolicyAuthenticatorAttachment("cross-platform")
                .setWebAuthnPolicyRequireResidentKey("No")
                .setWebAuthnPolicyRpId(null)
                .setWebAuthnPolicyUserVerificationRequirement("preferred")
                .setWebAuthnPolicyAcceptableAaguids(Collections.singletonList(ALL_ZERO_AAGUID));
    }
}
