/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console.authentication;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.auth.page.login.Registration;
import org.keycloak.testsuite.auth.page.login.TermsAndConditions;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.authentication.RequiredActions;
import org.keycloak.testsuite.console.page.realm.LoginSettings;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.By;

import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;

/**
 * @author Petr Mensik
 * @author mhajas
 */
public class RequiredActionsTest extends AbstractConsoleTest {

    @Page
    private RequiredActions requiredActionsPage;

    @Page
    private LoginSettings loginSettingsPage;

    @Page
    private Registration testRealmRegistrationPage;

    @Page
    private TermsAndConditions termsAndConditionsPage;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmRegistrationPage.setAuthRealm("test");
        termsAndConditionsPage.setAuthRealm("test");
    }

    @BeforeEach
    public void beforeRequiredActionsTest() {
        requiredActionsPage.navigateTo();
    }

    @Test
    public void termsAndConditionsDefaultActionTest() {
        requiredActionsPage.setTermsAndConditionEnabled(true);
        requiredActionsPage.setTermsAndConditionDefaultAction(true);
        assertAlertSuccess();

        allowTestRealmUserRegistration();

        navigateToTestRealmRegistration();

        registerTestUser();

        Assertions.assertTrue(termsAndConditionsPage.isCurrent());
    }

    @Test
    public void defaultCheckboxUncheckableWhenEnabledIsFalse() {
        requiredActionsPage.setTermsAndConditionEnabled(false);
        Assertions.assertFalse(requiredActionsPage.getTermsAndConditionEnabled());
        requiredActionsPage.setTermsAndConditionDefaultAction(true);
        Assertions.assertFalse(requiredActionsPage.getTermsAndConditionDefaultAction());
    }

    @Test
    public void defaultCheckboxUncheckedWhenEnabledBecomesFalse() {
        requiredActionsPage.setTermsAndConditionEnabled(true);
        Assertions.assertTrue(requiredActionsPage.getTermsAndConditionEnabled());
        requiredActionsPage.setTermsAndConditionDefaultAction(true);
        Assertions.assertTrue(requiredActionsPage.getTermsAndConditionDefaultAction());
        requiredActionsPage.setTermsAndConditionEnabled(false);
        Assertions.assertFalse(requiredActionsPage.getTermsAndConditionEnabled());
        Assertions.assertFalse(requiredActionsPage.getTermsAndConditionDefaultAction());
        assertAlertSuccess();
    }

    @Test
    public void defaultCheckboxKeepsValueWhenEnabledIsToggled() {
        requiredActionsPage.setTermsAndConditionEnabled(true);
        requiredActionsPage.setTermsAndConditionDefaultAction(false);
        Assertions.assertTrue(requiredActionsPage.getTermsAndConditionEnabled());
        Assertions.assertFalse(requiredActionsPage.getTermsAndConditionDefaultAction());
        requiredActionsPage.setTermsAndConditionEnabled(false);
        Assertions.assertFalse(requiredActionsPage.getTermsAndConditionEnabled());
        Assertions.assertFalse(requiredActionsPage.getTermsAndConditionDefaultAction());
        requiredActionsPage.setTermsAndConditionEnabled(true);
        Assertions.assertTrue(requiredActionsPage.getTermsAndConditionEnabled());
        Assertions.assertFalse(requiredActionsPage.getTermsAndConditionDefaultAction());

        requiredActionsPage.setTermsAndConditionDefaultAction(true);
        Assertions.assertTrue(requiredActionsPage.getTermsAndConditionEnabled());
        Assertions.assertTrue(requiredActionsPage.getTermsAndConditionDefaultAction());
        requiredActionsPage.setTermsAndConditionEnabled(false);
        Assertions.assertFalse(requiredActionsPage.getTermsAndConditionEnabled());
        Assertions.assertFalse(requiredActionsPage.getTermsAndConditionDefaultAction());
        requiredActionsPage.setTermsAndConditionEnabled(true);
        Assertions.assertTrue(requiredActionsPage.getTermsAndConditionEnabled());
        Assertions.assertTrue(requiredActionsPage.getTermsAndConditionDefaultAction());

        assertAlertSuccess();
    }

    @Test
    public void configureTotpDefaultActionTest() {
        requiredActionsPage.setConfigureTotpDefaultAction(true);
        assertAlertSuccess();

        allowTestRealmUserRegistration();

        navigateToTestRealmRegistration();

        registerTestUser();

        Assertions.assertTrue(UIUtils.getTextFromElement(driver.findElement(By.id("kc-page-title"))).equals("Mobile Authenticator Setup"));
    }

    private void allowTestRealmUserRegistration() {
        loginSettingsPage.navigateTo();
        loginSettingsPage.form().setRegistrationAllowed(true);
        loginSettingsPage.form().save();
    }

    private void navigateToTestRealmRegistration() {
        testRealmAdminConsolePage.navigateTo();
        testRealmLoginPage.form().register();
    }

    private void registerTestUser() {
        UserRepresentation user = createUserRepresentation("testUser", "testUser@email.test", "test", "user", true);
        setPasswordFor(user, PASSWORD);

        testRealmRegistrationPage.register(user);
    }
}
