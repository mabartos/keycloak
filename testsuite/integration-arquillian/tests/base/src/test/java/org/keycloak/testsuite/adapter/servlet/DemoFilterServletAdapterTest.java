/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.adapter.servlet;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.utils.annotation.UseServletFilter;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
@UseServletFilter(filterName = "oidc-filter", filterClass = "org.keycloak.adapters.servlet.KeycloakOIDCFilter",
        filterDependency = "org.keycloak:keycloak-servlet-filter-adapter", skipPattern = "/error.html")
public class DemoFilterServletAdapterTest extends DemoServletsAdapterTest {


    @Test
    @Override
    @Disabled(value = "Can't test because of the way filter works")
    public void testNullBearerTokenCustomErrorPage() {
    }
    
    @Test
    @Override
    @Disabled(value = "Don't need to test this because HttpServletRequest.authenticate "
            + "doesn't make sense with filter implementation")
    public void testAuthenticated() {
    }

    @Test
    @Override
    @Disabled
    public void testAuthenticatedWithCustomSessionConfig() {

    }

    @Test
    @Override
    @Disabled
    public void testOIDCParamsForwarding() {

    }

    @Test
    @Override
    @Disabled
    public void testOIDCUiLocalesParamForwarding() {

    }
    
    @Test
    @Override
    @Disabled
    public void testTokenInCookieSSO() {
        
    }
    
    @Test
    @Override
    @Disabled
    public void testInvalidTokenCookie() {
        
    }
    
    @Test
    @Override
    @Disabled
    public void testTokenInCookieRefresh() {
        
    }

    @Test
    @Override
    @Disabled
    public void testTokenInCookieSSORoot() {

    }
}
