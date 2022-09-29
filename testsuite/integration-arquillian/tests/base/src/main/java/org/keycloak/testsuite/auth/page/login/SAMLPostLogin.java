package org.keycloak.testsuite.auth.page.login;

import org.keycloak.testsuite.page.PageContext;

/**
 * @author mhajas
 */
public class SAMLPostLogin extends Login {
    SAMLPostLogin(PageContext pageContext) {
        super(pageContext);
        setProtocol(LOGIN_ACTION);
    }
}
