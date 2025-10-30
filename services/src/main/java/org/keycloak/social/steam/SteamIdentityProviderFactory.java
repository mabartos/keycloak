package org.keycloak.social.steam;

import org.keycloak.Config;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class SteamIdentityProviderFactory extends AbstractIdentityProviderFactory<SteamIdentityProvider>
        implements SocialIdentityProviderFactory<SteamIdentityProvider>, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "steam";

    @Override
    public String getName() {
        return "Steam";
    }

    @Override
    public SteamIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new SteamIdentityProvider(session, new OAuth2IdentityProviderConfig(model));
    }

    @Override
    public OAuth2IdentityProviderConfig createConfig() {
        return new OAuth2IdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.STEAM_BROKER);
    }
}
