package org.keycloak.testsuite.adapter.servlet.jetty;

import org.junit.jupiter.api.Disabled;
import org.keycloak.testsuite.adapter.servlet.DemoServletsAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

@AppServerContainer(ContainerConstants.APP_SERVER_JETTY94)
public class JettyDemoServletAdapterTest extends DemoServletsAdapterTest {

    @Disabled("KEYCLOAK-9614")
    @Override
    public void testAuthenticated() {

    }

    @Disabled("KEYCLOAK-9614")
    @Override
    public void testAuthenticatedWithCustomSessionConfig() {

    }

    @Disabled("KEYCLOAK-9616")
    @Override
    public void testOIDCParamsForwarding() {

    }

    @Disabled("KEYCLOAK-9616")
    @Override
    public void testOIDCUiLocalesParamForwarding() {

    }

    @Disabled("KEYCLOAK-9615")
    @Override
    public void testInvalidTokenCookie() {

    }

    @Disabled("KEYCLOAK-9615")
    @Override
    public void testTokenInCookieRefresh() {

    }

    @Disabled("KEYCLOAK-9615")
    @Override
    public void testTokenInCookieSSO() {

    }

    @Disabled("KEYCLOAK-9615")
    @Override
    public void testTokenInCookieSSORoot() {

    }

    @Disabled("KEYCLOAK-9617")
    @Override
    public void testWithoutKeycloakConf() {

    }
}
