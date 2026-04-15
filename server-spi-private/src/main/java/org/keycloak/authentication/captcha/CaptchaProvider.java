package org.keycloak.authentication.captcha;

import java.util.Map;

import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.provider.Provider;

/**
 * Provider interface for CAPTCHA integrations.
 *
 * <p>Implementations handle vendor-specific CAPTCHA logic including client-side widget rendering
 * and server-side token validation. Built-in implementations include Google reCAPTCHA (standard
 * and Enterprise). Third-party providers (hCaptcha, Cloudflare Turnstile, etc.) can be added
 * by implementing this interface.</p>
 */
public interface CaptchaProvider extends Provider {

    /**
     * Prepares the login form for CAPTCHA rendering.
     *
     * <p>Sets vendor-specific form attributes and adds necessary JavaScript resources so that
     * the CAPTCHA widget is rendered on the page.</p>
     *
     * @param form the login forms provider to set attributes on
     * @param config the provider configuration properties (site key, secret, etc.)
     */
    void prepareForm(LoginFormsProvider form, Map<String, String> config);

    /**
     * Validates a CAPTCHA response token with the vendor's server-side verification API.
     *
     * @param captchaResponse the token submitted by the client (from the CAPTCHA widget)
     * @param config the authenticator configuration properties (secret key, etc.)
     * @return {@code true} if the CAPTCHA response is valid
     */
    boolean validateToken(String captchaResponse, Map<String, String> config);

    /**
     * Returns the name of the form field that contains the CAPTCHA response token.
     *
     * <p>Each CAPTCHA vendor uses a different field name:</p>
     * <ul>
     *   <li>Google reCAPTCHA: {@code "g-recaptcha-response"}</li>
     *   <li>hCaptcha: {@code "h-captcha-response"}</li>
     *   <li>Cloudflare Turnstile: {@code "cf-turnstile-response"}</li>
     * </ul>
     *
     * @return the form field name for the CAPTCHA response token
     */
    String getResponseFormFieldName();

    /**
     * Validates that required configuration properties are present and valid.
     *
     * @param config the configuration map to validate
     * @return {@code true} if all required configuration is present and valid
     */
    boolean isConfigValid(Map<String, String> config);
}
