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

package org.keycloak.testsuite.webauthn;

import org.junit.After;
import org.junit.Before;
import org.keycloak.common.Profile;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.keycloak.testsuite.webauthn.authenticators.VirtualAuthenticatorManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.virtualauthenticator.HasVirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assume.assumeThat;

/**
 * Abstract class for WebAuthn tests which use Virtual Authenticators
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@EnableFeature(value = Profile.Feature.WEB_AUTHN, skipRestart = true, onlyForProduct = true)
public abstract class AbstractWebAuthnVirtualTest extends AbstractTestRealmKeycloakTest implements UseVirtualAuthenticators {

    private VirtualAuthenticatorManager virtualAuthenticatorsManager;

    @Before
    @Override
    public void setUpVirtualAuthenticator() {
        assumeThat("Driver must support Virtual Authenticators", driver, instanceOf(HasVirtualAuthenticator.class));
        this.virtualAuthenticatorsManager = createDefaultVirtualManager(driver, getDefaultAuthenticatorOptions());
        clearEventQueue();
    }

    @After
    public void removeVirtualAuthenticator() {
        virtualAuthenticatorsManager.removeAuthenticator();
        clearEventQueue();
    }

    public VirtualAuthenticatorOptions getDefaultAuthenticatorOptions() {
        return DefaultVirtualAuthOptions.DEFAULT;
    }

    public VirtualAuthenticatorManager getDefaultVirtualAuthManager() {
        return virtualAuthenticatorsManager;
    }

    public void getDefaultVirtualAuthManager(VirtualAuthenticatorManager manager) {
        this.virtualAuthenticatorsManager = manager;
    }

    protected void clearEventQueue() {
        getTestingClient().testing().clearEventQueue();
    }

    public static VirtualAuthenticatorManager createDefaultVirtualManager(WebDriver webDriver, VirtualAuthenticatorOptions options) {
        VirtualAuthenticatorManager manager = new VirtualAuthenticatorManager(webDriver);
        manager.useAuthenticator(options);
        return manager;
    }
}
