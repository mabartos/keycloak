package org.keycloak.authentication.captcha;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * SPI for CAPTCHA providers.
 *
 * <p>Allows pluggable CAPTCHA implementations (Google reCAPTCHA, hCaptcha, Cloudflare Turnstile, etc.)
 * to be used in authentication flows for registration, login, and password reset.</p>
 *
 */
public class CaptchaSpi implements Spi {

    public static final String NAME = "captcha";

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return CaptchaProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return CaptchaProviderFactory.class;
    }
}
