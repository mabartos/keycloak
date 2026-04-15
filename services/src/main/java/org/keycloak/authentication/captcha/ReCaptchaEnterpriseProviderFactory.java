package org.keycloak.authentication.captcha;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class ReCaptchaEnterpriseProviderFactory implements CaptchaProviderFactory {

    public static final String PROVIDER_ID = "recaptcha-enterprise";

    public static final String SITE_KEY = "site.key";
    public static final String PROJECT_ID = "project.id";
    public static final String API_KEY = "api.key";
    public static final String SCORE_THRESHOLD = "score.threshold";
    public static final String ACTION = "action";
    public static final String INVISIBLE = "recaptcha.v3";
    public static final String USE_RECAPTCHA_NET = "useRecaptchaNet";

    @Override
    public CaptchaProvider create(KeycloakSession session) {
        return new ReCaptchaEnterpriseProvider(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Google reCAPTCHA Enterprise";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(PROJECT_ID)
                .label("Project ID")
                .helpText("Project ID the site key belongs to.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(SITE_KEY)
                .label("reCAPTCHA Site Key")
                .helpText("The site key.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(API_KEY)
                .label("Google API Key")
                .helpText("An API key with the reCAPTCHA Enterprise API enabled in the given project ID.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .secret(true)
                .add()
                .property()
                .name(SCORE_THRESHOLD)
                .label("Min. Score Threshold")
                .helpText("The minimum score threshold for considering the reCAPTCHA valid (inclusive). "
                        + "Must be a valid double between 0.0 and 1.0.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("0.7")
                .add()
                .property()
                .name(ACTION)
                .label("Action Name")
                .helpText("A meaningful name for this reCAPTCHA context (e.g. login, register).")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("register")
                .add()
                .property()
                .name(USE_RECAPTCHA_NET)
                .label("Use recaptcha.net")
                .helpText("Whether to use recaptcha.net instead of google.com.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()
                .property()
                .name(INVISIBLE)
                .label("reCAPTCHA v3")
                .helpText("Whether the site key belongs to a v3 (invisible, score-based) reCAPTCHA.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()
                .build();
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
