package org.keycloak.authentication.captcha;

import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderFactory;

/**
 * Factory for creating {@link CaptchaProvider} instances.
 *
 * <p>Each CAPTCHA vendor (reCAPTCHA, hCaptcha, Turnstile, etc.) provides its own factory
 * implementation. The factory defines the configuration properties required by the vendor
 * (site key, secret key, etc.) and creates provider instances.</p>
 *
 * <p>Implementations must be registered via
 * {@code META-INF/services/org.keycloak.authentication.captcha.CaptchaProviderFactory}.</p>
 *
 */
public interface CaptchaProviderFactory extends ProviderFactory<CaptchaProvider>, ConfiguredProvider {
}
