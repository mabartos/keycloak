package org.keycloak.testsuite.adapter.servlet.jetty;

import org.junit.jupiter.api.Disabled;
import org.keycloak.testsuite.adapter.servlet.SAMLServletAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

@AppServerContainer(ContainerConstants.APP_SERVER_JETTY94)
public class JettySAMLServletAdapterTest extends SAMLServletAdapterTest {

    @Disabled("KEYCLOAK-9687")
    @Override
    public void multiTenant1SamlTest() throws Exception {

    }

    @Disabled("KEYCLOAK-9687")
    @Override
    public void multiTenant2SamlTest() throws Exception {

    }
}
