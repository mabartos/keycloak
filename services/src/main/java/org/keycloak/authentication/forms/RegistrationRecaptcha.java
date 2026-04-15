package org.keycloak.authentication.forms;

import java.util.List;

import org.keycloak.authentication.captcha.ReCaptchaProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Registration form action for Google reCAPTCHA v2/v3.
 *
 * <p>Delegates all CAPTCHA logic to the {@code recaptcha} {@link org.keycloak.authentication.captcha.CaptchaProvider}.</p>
 */
public class RegistrationRecaptcha extends AbstractCaptchaFormAction {

    public static final String PROVIDER_ID = "registration-recaptcha-action";

    @Override
    protected String getCaptchaProviderId() {
        return ReCaptchaProviderFactory.PROVIDER_ID;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "reCAPTCHA";
    }

    @Override
    public String getHelpText() {
        return "Adds Google reCAPTCHA to the form.";
    }

    @Override
    public String getReferenceCategory() {
        return "recaptcha";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new ReCaptchaProviderFactory().getConfigProperties();
    }
}
