package org.keycloak.authentication.captcha;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.logging.Logger;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.validation.Validation;

/**
 * Utility for integrating CAPTCHA with login and reset-password flows.
 *
 * <p>Checks realm-level CAPTCHA configuration (stored as realm attributes) and delegates
 * to the configured {@link CaptchaProvider} for form preparation and token validation.</p>
 *
 * <p>When CAPTCHA is not enabled (no realm attributes set), all methods are no-ops,
 * ensuring no breaking changes to existing flows.</p>
 */
public class CaptchaHelper {

    private static final Logger logger = Logger.getLogger(CaptchaHelper.class);

    public static final String CAPTCHA_ENABLED = "captchaEnabled";
    public static final String CAPTCHA_PROVIDER = "captchaProvider";
    private static final String CAPTCHA_CONFIG_PREFIX = "captcha.";

    private CaptchaHelper() {
    }

    /**
     * Check if CAPTCHA is enabled for this realm.
     */
    public static boolean isCaptchaEnabled(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        return Boolean.parseBoolean(realm.getAttribute(CAPTCHA_ENABLED));
    }

    /**
     * Add CAPTCHA widget to a form if enabled for the realm.
     * No-op if CAPTCHA is not configured.
     *
     * @param session the Keycloak session
     * @param form the login forms provider
     * @param defaultAction the default action name (e.g. "login", "reset")
     */
    public static void addCaptchaToFormIfEnabled(KeycloakSession session, LoginFormsProvider form, String defaultAction) {
        if (!isCaptchaEnabled(session)) {
            return;
        }

        CaptchaProvider provider = getCaptchaProvider(session);
        Map<String, String> config = getCaptchaConfig(session);
        if (provider == null || !provider.isConfigValid(config)) {
            logger.debug("CAPTCHA is enabled but provider is not configured properly");
            return;
        }

        config.putIfAbsent("action", defaultAction);
        provider.prepareForm(form, config);
    }

    /**
     * Validate CAPTCHA response from form data if enabled.
     * Returns {@code true} if CAPTCHA is valid OR if CAPTCHA is not enabled.
     *
     * @param session the Keycloak session
     * @param formData the submitted form data
     * @param remoteAddr the client's remote IP address
     * @return {@code true} if valid or not enabled
     */
    public static boolean validateCaptchaIfEnabled(KeycloakSession session, MultivaluedMap<String, String> formData) {
        if (!isCaptchaEnabled(session)) {
            return true;
        }

        CaptchaProvider provider = getCaptchaProvider(session);
        Map<String, String> config = getCaptchaConfig(session);
        if (provider == null || !provider.isConfigValid(config)) {
            return true;
        }

        String token = formData.getFirst(provider.getResponseFormFieldName());
        if (Validation.isBlank(token)) {
            logger.debug("CAPTCHA response token is missing");
            return false;
        }

        return provider.validateToken(token, config);
    }

    private static CaptchaProvider getCaptchaProvider(KeycloakSession session) {
        String providerId = session.getContext().getRealm().getAttribute(CAPTCHA_PROVIDER);
        if (providerId == null) {
            return null;
        }
        return session.getProvider(CaptchaProvider.class, providerId);
    }

    private static Map<String, String> getCaptchaConfig(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        return realm.getAttributes().entrySet().stream()
                .filter(e -> e.getKey().startsWith(CAPTCHA_CONFIG_PREFIX))
                .collect(Collectors.toMap(
                        e -> e.getKey().substring(CAPTCHA_CONFIG_PREFIX.length()),
                        Map.Entry::getValue
                ));
    }
}
