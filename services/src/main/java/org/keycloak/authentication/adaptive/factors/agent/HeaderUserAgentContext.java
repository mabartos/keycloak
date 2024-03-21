package org.keycloak.authentication.adaptive.factors.agent;

import jakarta.ws.rs.core.HttpHeaders;
import org.keycloak.models.KeycloakSession;

public class HeaderUserAgentContext implements UserAgentContext {
    private final KeycloakSession session;
    private boolean isInitialized;
    private UserAgent data;

    public HeaderUserAgentContext(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean isDataInitialized() {
        return isInitialized;
    }

    @Override
    public void initData() {
        final String agent = session.getContext()
                .getRequestHeaders()
                .getHeaderString(HttpHeaders.USER_AGENT);

        this.data = UserAgent.knownAgents
                .stream()
                .filter(f -> f.getName().contains(agent))
                .findAny()
                .orElse(() -> agent);

        this.isInitialized = true;
    }

    @Override
    public UserAgent getData() {
        return data;
    }
}
