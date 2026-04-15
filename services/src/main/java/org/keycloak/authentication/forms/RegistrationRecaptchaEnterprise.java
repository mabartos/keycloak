package org.keycloak.authentication.forms;

import java.util.List;

import org.keycloak.authentication.captcha.ReCaptchaEnterpriseProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Registration form action for Google reCAPTCHA Enterprise.
 *
 * <p>Delegates all CAPTCHA logic to the {@code recaptcha-enterprise}
 * {@link org.keycloak.authentication.captcha.CaptchaProvider}.</p>
 */
public class RegistrationRecaptchaEnterprise extends AbstractCaptchaFormAction {

    public static final String PROVIDER_ID = "registration-recaptcha-enterprise";

    @Override
    protected String getCaptchaProviderId() {
        return ReCaptchaEnterpriseProviderFactory.PROVIDER_ID;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "reCAPTCHA Enterprise";
    }

    @Override
    public String getHelpText() {
        return "Adds Google reCAPTCHA Enterprise to the form.";
    }

    @Override
    public String getReferenceCategory() {
        return "recaptcha";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new ReCaptchaEnterpriseProviderFactory().getConfigProperties();
    }
}
