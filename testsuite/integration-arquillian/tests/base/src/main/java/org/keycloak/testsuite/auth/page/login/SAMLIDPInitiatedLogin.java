package org.keycloak.testsuite.auth.page.login;

import org.keycloak.testsuite.page.PageContext;

import javax.ws.rs.core.UriBuilder;

/**
 * @author mhajas
 */
public class SAMLIDPInitiatedLogin extends SAMLRedirectLogin {

    SAMLIDPInitiatedLogin(PageContext pageContext) {
        super(pageContext);
    }

    public void setUrlName(String urlName) {
        setUriParameter("clientUrlName", urlName);
    }

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("clients/{clientUrlName}");
    }
}

