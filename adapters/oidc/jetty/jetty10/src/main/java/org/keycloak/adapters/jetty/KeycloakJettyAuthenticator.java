package org.keycloak.adapters.jetty;

import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.jetty.core.AbstractKeycloakJettyAuthenticator;
import org.keycloak.adapters.jetty.core.JettyRequestAuthenticator;
import org.keycloak.adapters.jetty.core.JettySessionTokenStore;
import org.keycloak.adapters.jetty.spi.JettyHttpFacade;
import org.keycloak.adapters.jetty.spi.JettyUserSessionManagement;

import javax.servlet.ServletRequest;

public class KeycloakJettyAuthenticator extends AbstractKeycloakJettyAuthenticator {

    public KeycloakJettyAuthenticator() {
        super();
    }

    @Override
    public AdapterTokenStore createSessionTokenStore(Request request, KeycloakDeployment resolvedDeployment) {
        return new JettySessionTokenStore(request, resolvedDeployment, new JettyAdapterSessionStore(request));
    }

    @Override
    public JettyUserSessionManagement createSessionManagement(Request request) {
        return new JettyUserSessionManagement(new Jetty10SessionManager(request.getSessionHandler()));
    }

    @Override
    protected Request resolveRequest(ServletRequest req) {
        return Request.getBaseRequest(req);
    }

    @Override
    protected Authentication createAuthentication(UserIdentity userIdentity, Request request) {
        return new KeycloakAuthentication(getAuthMethod(), userIdentity) {
            @Override
            public Authentication logout(ServletRequest servletRequest) {
                logoutCurrent((Request) servletRequest);
                return super.logout(servletRequest);
            }
        };
    }

    @Override
    protected JettyRequestAuthenticator createRequestAuthenticator(Request request, JettyHttpFacade facade,
                                                                   KeycloakDeployment deployment, AdapterTokenStore tokenStore) {
        return new Jetty10RequestAuthenticator(facade, deployment, tokenStore, -1, request);
    }
}
