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

import org.junit.After;
import org.junit.Before;
import org.keycloak.common.Profile;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.keycloak.testsuite.webauthn.authenticators.UseVirtualAuthenticators;
import org.keycloak.testsuite.webauthn.authenticators.VirtualAuthenticatorManager;

@EnableFeature(value = Profile.Feature.WEB_AUTHN, skipRestart = true, onlyForProduct = true)
public abstract class AbstractWebAuthnAccountTest extends AbstractAuthTest implements UseVirtualAuthenticators {

    protected abstract AbstractLoggedInPage getAccountPage();

    private VirtualAuthenticatorManager webAuthnManager;

    @Override
    @Before
    public void setUpVirtualAuthenticator() {
        webAuthnManager = AbstractWebAuthnVirtualTest.createDefaultVirtualManager(driver,DefaultVirtualAuthOptions.DEFAULT);
    }

    @Override
    @After
    public void removeVirtualAuthenticator() {
        webAuthnManager.removeAuthenticator();
    }

    @Before
    public void navigateBeforeTest() {
        createTestUserWithAdminClient(false);

        getAccountPage().navigateTo();
        loginToAccount();
        getAccountPage().assertCurrent();
    }

    protected void loginToAccount() {
        loginPage.assertCurrent();
        loginPage.form().login(testUser);
    }

    protected VirtualAuthenticatorManager getWebAuthnManager() {
        return webAuthnManager;
    }
}
