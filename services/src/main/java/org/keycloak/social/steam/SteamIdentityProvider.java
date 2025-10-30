package org.keycloak.social.steam;

import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.models.KeycloakSession;

public class SteamIdentityProvider extends AbstractOAuth2IdentityProvider implements SocialIdentityProvider {

    public static final String AUTH_URL = "https://steamcommunity.com/oauth/login";
    public static final String TOKEN_URL = "https://steamcommunity.com/openid/";
    public static final String PROFILE_URL = "https://api.stackexchange.com/2.2/me?order=desc&sort=name&site=stackoverflow";
    public static final String DEFAULT_SCOPE = "";


    public SteamIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
        super(session, config);
        config.setAuthorizationUrl(AUTH_URL);
        config.setTokenUrl(TOKEN_URL);
        config.setUserInfoUrl(PROFILE_URL);
    }

    @Override
    protected String getDefaultScopes() {
        return "openid";
    }
}

