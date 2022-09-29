package org.keycloak.testsuite.admin;

import org.junit.Test;
import org.keycloak.testsuite.page.Page;
import org.keycloak.testsuite.pages.LoginPage;

public class TestTest extends AbstractAdminTest {

    @Page
    LoginPage page;

    @Test
    public void testTest() {
        page.open();
        page.assertCurrent();
    }
}
