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

package org.keycloak.testsuite.forms;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.Constants;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.authentication.PushButtonAuthenticatorFactory;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.pages.PasswordPage;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for Level Of Assurance conditions in authentication flow.
 *
 * @author <a href="mailto:sebastian.zoescher@prime-sign.com">Sebastian Zoescher</a>
 */
public class LevelOfAssuranceFlowTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginUsernameOnlyPage loginUsernameOnlyPage;

    @Page
    protected PasswordPage passwordPage;

    private FluentWait<WebDriver> wait;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        try {
            Map<String, Integer> acrLoaMap = new HashMap<>();
            acrLoaMap.put("copper", 0);
            acrLoaMap.put("silver", 1);
            acrLoaMap.put("gold", 2);
            findTestApp(testRealm).setAttributes(Collections.singletonMap(Constants.ACR_LOA_MAP, JsonSerialization.writeValueAsString(acrLoaMap)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void afterAbstractKeycloakTestRealmImport() {
        super.afterAbstractKeycloakTestRealmImport();

        final String newFlowAlias = "browser -  Level of Authentication FLow";

        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setId(newFlowAlias);
        flow.setAlias(newFlowAlias);
        flow.setProviderId("basic-flow");
        flow.setBuiltIn(false);
        flow.setTopLevel(true);
        testRealm().flows().createFlow(flow);

        // Level 1
        AuthenticationExecutionRepresentation level1Flow = new AuthenticationExecutionRepresentation();
        level1Flow.setParentFlow(newFlowAlias);
        level1Flow.setFlowId("level1");
        level1Flow.setRequirement(Requirement.CONDITIONAL.name());
        level1Flow.setAuthenticatorFlow(true);
        testRealm().flows().addExecution(level1Flow);

        // Level 1 LoA
        AuthenticationExecutionRepresentation level1LoA = new AuthenticationExecutionRepresentation();
        level1LoA.setParentFlow("level1");
        level1LoA.setRequirement(Requirement.REQUIRED.name());
        level1LoA.setAuthenticator(ConditionalLoaAuthenticatorFactory.PROVIDER_ID);
        level1LoA.setAuthenticatorFlow(false);

        AuthenticatorConfigRepresentation level1Config = new AuthenticatorConfigRepresentation();
        level1Config.setAlias("level1Config");
        level1Config.setId("level1Config");
        final Map<String, String> config = new HashMap<>();
        config.put(ConditionalLoaAuthenticator.LEVEL, "1");
        config.put(ConditionalLoaAuthenticator.STORE_IN_USER_SESSION, "true");
        level1Config.setConfig(config);

        level1LoA.setAuthenticatorConfig("level1Config");
        testRealm().flows().addExecution(level1LoA);

        // Level 1 Username
        AuthenticationExecutionRepresentation level1Username = new AuthenticationExecutionRepresentation();
        level1LoA.setParentFlow("level1");
        level1LoA.setRequirement(Requirement.REQUIRED.name());
        level1LoA.setAuthenticator(UsernameFormFactory.PROVIDER_ID);
        level1LoA.setAuthenticatorFlow(false);
        testRealm().flows().addExecution(level1Username);

        // Level 2
        AuthenticationExecutionRepresentation level2Flow = new AuthenticationExecutionRepresentation();
        level2Flow.setParentFlow(newFlowAlias);
        level2Flow.setFlowId("level2");
        level2Flow.setRequirement(Requirement.CONDITIONAL.name());
        level2Flow.setAuthenticatorFlow(true);
        testRealm().flows().addExecution(level2Flow);

        AuthenticationExecutionRepresentation level2LoA = new AuthenticationExecutionRepresentation();
        level2LoA.setParentFlow("level2");
        level2LoA.setRequirement(Requirement.REQUIRED.name());
        level2LoA.setAuthenticator(ConditionalLoaAuthenticatorFactory.PROVIDER_ID);
        level2LoA.setAuthenticatorFlow(false);

        AuthenticatorConfigRepresentation level2Config = new AuthenticatorConfigRepresentation();
        level2Config.setAlias("level2Config");
        level2Config.setId("level2Config");
        final Map<String, String> config2 = new HashMap<>();
        config.put(ConditionalLoaAuthenticator.LEVEL, "2");
        level2Config.setConfig(config2);

        level1LoA.setAuthenticatorConfig("level2Config");
        testRealm().flows().addExecution(level2LoA);

        AuthenticationExecutionRepresentation level2Password = new AuthenticationExecutionRepresentation();
        level1LoA.setParentFlow("level2");
        level1LoA.setRequirement(Requirement.REQUIRED.name());
        level1LoA.setAuthenticator(PasswordFormFactory.PROVIDER_ID);
        level1LoA.setAuthenticatorFlow(false);
        testRealm().flows().addExecution(level2Password);

        // Level 3
        AuthenticationExecutionRepresentation level3Flow = new AuthenticationExecutionRepresentation();
        level3Flow.setParentFlow(newFlowAlias);
        level3Flow.setFlowId("level3");
        level3Flow.setRequirement(Requirement.CONDITIONAL.name());
        level3Flow.setAuthenticatorFlow(true);
        testRealm().flows().addExecution(level3Flow);

        AuthenticationExecutionRepresentation level3LoA = new AuthenticationExecutionRepresentation();
        level3LoA.setParentFlow("level3");
        level3LoA.setRequirement(Requirement.REQUIRED.name());
        level3LoA.setAuthenticator(ConditionalLoaAuthenticatorFactory.PROVIDER_ID);
        level3LoA.setAuthenticatorFlow(false);

        AuthenticatorConfigRepresentation level3Config = new AuthenticatorConfigRepresentation();
        level3Config.setAlias("level3Config");
        level3Config.setId("level3Config");
        final Map<String, String> config3 = new HashMap<>();
        config.put(ConditionalLoaAuthenticator.LEVEL, "3");
        level3Config.setConfig(config3);

        level1LoA.setAuthenticatorConfig("level3Config");
        testRealm().flows().addExecution(level3LoA);

        AuthenticationExecutionRepresentation level3PushButton = new AuthenticationExecutionRepresentation();
        level1LoA.setParentFlow("level2");
        level1LoA.setRequirement(Requirement.REQUIRED.name());
        level1LoA.setAuthenticator(PushButtonAuthenticatorFactory.PROVIDER_ID);
        level1LoA.setAuthenticatorFlow(false);
        testRealm().flows().addExecution(level3PushButton);

        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setBrowserFlow(newFlowAlias);

        testRealm().update(realm);
    }

   /* @Before
    public void setupFlow() {
        wait = new FluentWait<>(driver).withTimeout(Duration.ofSeconds(10));
        final String newFlowAlias = "browser -  Level of Authentication FLow";
        testingClient.server(TEST_REALM_NAME).run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server(TEST_REALM_NAME)
                .run(session -> FlowUtil.inCurrentRealm(session).selectFlow(newFlowAlias).inForms(forms -> forms.clear()
                        // level 1 authentication
                        .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "1");
                                        config.getConfig().put(ConditionalLoaAuthenticator.STORE_IN_USER_SESSION, "true");
                                    });

                            // username input for level 1
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID);
                        })

                        // level 2 authentication
                        .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "2"));

                            // password required for level 2
                            subFlow.addAuthenticatorExecution(Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID);
                        })

                // level 3 authentication
                .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                    subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                        config -> config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "3"));

                    // simply push button for level 3
                    subFlow.addAuthenticatorExecution(Requirement.REQUIRED, PushButtonAuthenticatorFactory.PROVIDER_ID);
                })

            ).defineAsBrowserFlow());
    }*/

    @Test
    public void loginWithoutAcr() {
        oauth.openLoginForm();
        // Authentication without specific LOA results in level 1 authentication
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");
    }

    @Test
    public void loginWithAcr1() {
        // username input for level 1
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");

    }

    @Test
    public void loginWithAcr2() {
        // username and password input for level 2
        openLoginFormWithAcrClaim(true, "gold");
        authenticateWithUsername();
        authenticateWithPassword();
        assertLoggedInWithAcr("gold");
    }

    @Test
    public void loginWithAcr3() {
        // username, password input and finally push button for level 3
        openLoginFormWithAcrClaim(true, "3");
        authenticateWithUsername();
        authenticateWithPassword();
        authenticateWithButton();
        // ACR 3 is returned because it was requested, although there is no mapping for it
        assertLoggedInWithAcr("3");
    }

    @Test
    public void stepupAuthentication() {
        // logging in to level 1
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");
        // doing step-up authentication to level 2
        openLoginFormWithAcrClaim(true, "gold");
        authenticateWithPassword();
        authenticateWithButton();
        assertLoggedInWithAcr("gold");
        // step-up to level 3 needs password authentication because level 2 is not stored in user session
        openLoginFormWithAcrClaim(true, "3");
        authenticateWithPassword();
        authenticateWithButton();
        assertLoggedInWithAcr("3");
    }

    @Test
    public void reauthenticationWithNoAcr() {
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");
        oauth.openLoginForm();
        assertLoggedInWithAcr("0");
    }

    @Test
    public void reauthenticationWithReachedAcr() {
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");
        openLoginFormWithAcrClaim(true, "silver");
        assertLoggedInWithAcr("0");
    }

    @Test
    public void reauthenticationWithOptionalUnknownAcr() {
        openLoginFormWithAcrClaim(true, "silver");
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");
        openLoginFormWithAcrClaim(false, "iron");
        assertLoggedInWithAcr("0");
    }

    @Test
    public void optionalClaimNotReachedSucceeds() {
        openLoginFormWithAcrClaim(false, "4");
        authenticateWithUsername();
        authenticateWithPassword();
        authenticateWithButton();
        // the reached loa is 3, but there is no mapping for it, and it was not explicitly
        // requested, so the highest known and reached ACR is returned
        assertLoggedInWithAcr("gold");
    }

    @Test
    public void optionalUnknownClaimSucceeds() {
        openLoginFormWithAcrClaim(false, "iron");
        authenticateWithUsername();
        assertLoggedInWithAcr("silver");
    }

    @Test
    public void acrValuesQueryParameter() {
        driver.navigate().to(UriBuilder.fromUri(oauth.getLoginFormUrl())
            .queryParam("acr_values", "gold 3")
            .build().toString());
        authenticateWithUsername();
        authenticateWithPassword();
        assertLoggedInWithAcr("gold");
    }

    @Test
    public void multipleEssentialAcrValues() {
        openLoginFormWithAcrClaim(true, "gold", "3");
        authenticateWithUsername();
        authenticateWithPassword();
        assertLoggedInWithAcr("gold");
    }

    @Test
    public void multipleOptionalAcrValues() {
        openLoginFormWithAcrClaim(false, "gold", "3");
        authenticateWithUsername();
        authenticateWithPassword();
        assertLoggedInWithAcr("gold");
    }

    private void openLoginFormWithAcrClaim(boolean essential, String... acrValues) {
        ClaimsRepresentation.ClaimValue<String> acrClaim = new ClaimsRepresentation.ClaimValue<>();
        acrClaim.setEssential(essential);
        acrClaim.setValues(Arrays.asList(acrValues));

        ClaimsRepresentation claims = new ClaimsRepresentation();
        claims.setIdTokenClaims(Collections.singletonMap("acr", acrClaim));

        try {
            driver.navigate().to(UriBuilder.fromUri(oauth.getLoginFormUrl())
                .queryParam("claims", "{0}")
                .build(JsonSerialization.writeValueAsString(claims)).toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void authenticateWithUsername() {
        loginUsernameOnlyPage.assertCurrent();
        loginUsernameOnlyPage.login("test-user@localhost");
    }

    private void authenticateWithPassword() {
        passwordPage.assertCurrent();
        passwordPage.login("password");
    }

    private void authenticateWithButton() {
        driver.findElement(By.name("submit1")).click();
    }

    private void assertLoggedInWithAcr(String acr) {
        EventRepresentation loginEvent = events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
        IDToken idToken = sendTokenRequestAndGetIDToken(loginEvent);
        Assert.assertEquals(acr, idToken.getAcr());
    }
}
