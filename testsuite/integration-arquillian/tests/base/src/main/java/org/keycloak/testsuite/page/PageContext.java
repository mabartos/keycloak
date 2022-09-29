/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.page;

import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.TestContext;
import org.keycloak.testsuite.util.OAuthClient;
import org.openqa.selenium.WebDriver;

public class PageContext {
    private final OAuthClient oAuthClient;
    private final SuiteContext suiteContext;
    private final TestContext testContext;
    private final WebDriver webDriver;

    public PageContext(OAuthClient oAuthClient, SuiteContext suiteContext, TestContext testContext, WebDriver webDriver) {
        this.oAuthClient = oAuthClient;
        this.suiteContext = suiteContext;
        this.testContext = testContext;
        this.webDriver = webDriver;
    }

    public OAuthClient getOAuthClient() {
        return oAuthClient;
    }

    public SuiteContext getSuiteContext() {
        return suiteContext;
    }

    public TestContext getTestContext() {
        return testContext;
    }

    public WebDriver getWebDriver() {
        return webDriver;
    }
}
