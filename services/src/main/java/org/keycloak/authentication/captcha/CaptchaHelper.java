package org.keycloak.authentication.captcha;

import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.validation.Validation;

/**
 * Utility for integrating CAPTCHA with login and reset-password flows.
 *
 * <p>Resolves the default CAPTCHA provider instance from realm components and delegates
 * to the configured {@link CaptchaProvider} for form preparation and token validation.</p>
 *
 * <p>When no default CAPTCHA instance is configured, all methods are no-ops,
 * ensuring no breaking changes to existing flows.</p>
 */
public class CaptchaHelper {

    private static final Logger logger = Logger.getLogger(CaptchaHelper.class);

    private CaptchaHelper() {
    }

    /**
     * Check if a default CAPTCHA instance is configured for this realm.
     */
    public static boolean isCaptchaEnabled(KeycloakSession session) {
        return getDefaultComponentId(session) != null;
    }

    /**
     * Add CAPTCHA widget to a form if a default instance is configured.
     * No-op if no CAPTCHA is configured.
     *
     * @param session the Keycloak session
     * @param form the login forms provider
     * @param defaultAction the default action name (e.g. "login", "reset")
     */
    public static void addCaptchaToFormIfEnabled(KeycloakSession session, LoginFormsProvider form, String defaultAction) {
        ComponentModel component = getDefaultComponent(session);
        if (component == null) {
            return;
        }

        CaptchaProvider provider = session.getProvider(CaptchaProvider.class, component.getProviderId());
        Map<String, String> config = CaptchaConfigService.toInstance(component).getConfig();

        if (provider == null || !provider.isConfigValid(config)) {
            logger.debug("CAPTCHA is enabled but provider is not configured properly");
            return;
        }

        config.putIfAbsent("action", defaultAction);
        provider.prepareForm(form, config);
    }

    /**
     * Validate CAPTCHA response from form data if a default instance is configured.
     * Returns {@code true} if CAPTCHA is valid OR if no CAPTCHA is configured.
     *
     * @param session the Keycloak session
     * @param formData the submitted form data
     * @return {@code true} if valid or not configured
     */
    public static boolean validateCaptchaIfEnabled(KeycloakSession session, MultivaluedMap<String, String> formData) {
        ComponentModel component = getDefaultComponent(session);
        if (component == null) {
            return true;
        }

        CaptchaProvider provider = session.getProvider(CaptchaProvider.class, component.getProviderId());
        Map<String, String> config = CaptchaConfigService.toInstance(component).getConfig();

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

    /**
     * Get the component ID of the default CAPTCHA instance, or {@code null} if not set.
     */
    static String getDefaultComponentId(KeycloakSession session) {
        return session.getContext().getRealm().getAttribute(CaptchaConstants.CAPTCHA_DEFAULT);
    }

    /**
     * Load the default CAPTCHA component, or {@code null} if not found.
     */
    private static ComponentModel getDefaultComponent(KeycloakSession session) {
        String id = getDefaultComponentId(session);
        if (id == null) {
            return null;
        }

        RealmModel realm = session.getContext().getRealm();
        ComponentModel model = realm.getComponent(id);
        if (model == null || !CaptchaProvider.class.getName().equals(model.getProviderType())) {
            logger.warnf("Default CAPTCHA instance '%s' not found or not a CAPTCHA component", id);
            return null;
        }

        return model;
    }
}
