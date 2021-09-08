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

package org.keycloak.testsuite.webauthn.authenticators;

import org.hamcrest.CoreMatchers;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.virtualauthenticator.HasVirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Manager for Virtual Authenticators
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class VirtualAuthenticatorsManager {
    private static VirtualAuthenticatorsManager INSTANCE;

    private HasVirtualAuthenticator driver;
    private KcVirtualAuthenticator actualAuthenticator;

    private VirtualAuthenticatorsManager(WebDriver driver) {
        this.driver = getVirtualAuthenticator(driver);
        this.actualAuthenticator = null;
    }

    public static VirtualAuthenticatorsManager getInstance(WebDriver driver) {
        if (INSTANCE == null) {
            INSTANCE = new VirtualAuthenticatorsManager(driver);
            return INSTANCE;
        }

        // Change driver for manager
        if (!driver.equals(INSTANCE.driver)) {
            INSTANCE.driver = getVirtualAuthenticator(driver);
        }

        return INSTANCE;
    }

    public KcVirtualAuthenticator useAuthenticator(VirtualAuthenticatorOptions options) {
        this.actualAuthenticator = new KcVirtualAuthenticator(driver.addVirtualAuthenticator(options), options);
        return actualAuthenticator;
    }

    public KcVirtualAuthenticator getActualAuthenticator() {
        return actualAuthenticator;
    }

    public void removeAuthenticator() {
        if (actualAuthenticator != null) {
            driver.removeVirtualAuthenticator(actualAuthenticator.getAuthenticator());
        }
    }

    protected static HasVirtualAuthenticator getVirtualAuthenticator(WebDriver driver) {
        assertThat("Driver must support Virtual Authenticators", driver, CoreMatchers.instanceOf(HasVirtualAuthenticator.class));
        return (HasVirtualAuthenticator) driver;
    }
}
