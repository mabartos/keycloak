package org.keycloak.authentication.forms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.authentication.captcha.CaptchaProvider;
import org.keycloak.authentication.captcha.CaptchaProviderFactory;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;

/**
 * Base FormAction for CAPTCHA validation in registration flows.
 *
 * <p>Subclasses specify which {@link CaptchaProvider} to use via {@link #getCaptchaProviderId()}.
 * All CAPTCHA-specific logic (form rendering, token validation) is delegated to the provider.</p>
 */
public abstract class AbstractCaptchaFormAction implements FormAction, FormActionFactory {

    private static final Logger logger = Logger.getLogger(AbstractCaptchaFormAction.class);

    /**
     * Returns the CaptchaProvider factory ID to use (e.g., "recaptcha", "recaptcha-enterprise").
     */
    protected abstract String getCaptchaProviderId();

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        logger.trace("Building page with CAPTCHA");

        Map<String, String> config = null;
        if (context.getAuthenticatorConfig() != null) {
            config = context.getAuthenticatorConfig().getConfig();
        }

        CaptchaProvider captcha = context.getSession().getProvider(CaptchaProvider.class, getCaptchaProviderId());
        if (captcha == null || config == null || !captcha.isConfigValid(config)) {
            form.addError(new FormMessage(null, Messages.RECAPTCHA_NOT_CONFIGURED));
            return;
        }

        captcha.prepareForm(form, config);
    }

    @Override
    public void validate(ValidationContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        Map<String, String> config = context.getAuthenticatorConfig().getConfig();

        CaptchaProvider captcha = context.getSession().getProvider(CaptchaProvider.class, getCaptchaProviderId());
        String token = formData.getFirst(captcha.getResponseFormFieldName());
        logger.trace("Got captcha token: " + token);

        if (!Validation.isBlank(token) && captcha.validateToken(token, config)) {
            context.success();
            return;
        }

        List<FormMessage> errors = new ArrayList<>();
        errors.add(new FormMessage(null, Messages.RECAPTCHA_FAILED));
        context.error(Errors.INVALID_REGISTRATION);
        context.validationError(formData, errors);
        context.excludeOtherErrors();
    }

    @Override
    public void success(FormContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] {
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public FormAction create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }
}
