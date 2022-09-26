package org.keycloak.testsuite.adapter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.testsuite.adapter.servlet.SAMLFilterServletAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

@AppServerContainer("app-server-was")
public class WASSAMLFilterAdapterTest extends SAMLFilterServletAdapterTest {
    @Override
    @Disabled // KEYCLOAK-6152
    @Test
    public void testPostBadAssertionSignature() {}

    @Override
    @Disabled // KEYCLOAK-6152
    @Test
    public void salesPostEncRejectConsent() {}

    @Override
    @Disabled // KEYCLOAK-6152
    @Test
    public void salesPostRejectConsent() {}

    @Override
    @Disabled // KEYCLOAK-6152
    @Test
    public void testDifferentCookieName() {}

    @Override
    @Disabled
    @Test
    public void testMissingAssertionSignature() {}

    @Override
    @Disabled // KEYCLOAK-6152
    @Test
    public void testRelayStateEncoding() {}
}
