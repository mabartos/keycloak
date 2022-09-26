package org.keycloak.testsuite.adapter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.testsuite.adapter.servlet.SAMLFilterServletAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

@AppServerContainer("app-server-wls")
public class WLSSAMLFilterAdapterTest extends SAMLFilterServletAdapterTest {

    @Disabled // KEYCLOAK-6152
    @Override
    @Test
    public void testDifferentCookieName() {}
}
