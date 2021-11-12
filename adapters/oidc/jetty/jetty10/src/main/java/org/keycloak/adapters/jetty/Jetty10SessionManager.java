package org.keycloak.adapters.jetty;

import org.eclipse.jetty.server.session.Session;
import org.eclipse.jetty.server.session.SessionHandler;
import org.keycloak.adapters.jetty.spi.JettySessionManager;

import javax.servlet.http.HttpSession;

public class Jetty10SessionManager implements JettySessionManager {
    protected SessionHandler sessionHandler;

    public Jetty10SessionManager(SessionHandler sessionHandler) {
        this.sessionHandler = sessionHandler;
    }

    @Override
    public HttpSession getHttpSession(String extendedId) {
        // inlined code from sessionHandler.getHttpSession(extendedId) since the method visibility changed to protected

        String id = sessionHandler.getSessionIdManager().getId(extendedId);
        Session session = sessionHandler.getSession(id);

        if (session != null && !session.getExtendedId().equals(extendedId)) {
            session.setIdChanged(true);
        }
        return session;
    }
}
