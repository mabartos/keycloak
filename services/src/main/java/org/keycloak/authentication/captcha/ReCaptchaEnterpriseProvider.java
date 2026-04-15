package org.keycloak.authentication.captcha;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.forms.RecaptchaAssessmentRequest;
import org.keycloak.authentication.forms.RecaptchaAssessmentResponse;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import static org.keycloak.authentication.captcha.CaptchaConstants.CAPTCHA_ACTION;
import static org.keycloak.authentication.captcha.CaptchaConstants.CAPTCHA_REQUIRED;
import static org.keycloak.authentication.captcha.CaptchaConstants.CAPTCHA_SITE_KEY;
import static org.keycloak.authentication.captcha.CaptchaConstants.CAPTCHA_VISIBLE;
import static org.keycloak.authentication.captcha.CaptchaConstants.CAPTCHA_WIDGET_CLASS;
import static org.keycloak.authentication.captcha.ReCaptchaEnterpriseProviderFactory.ACTION;
import static org.keycloak.authentication.captcha.ReCaptchaEnterpriseProviderFactory.API_KEY;
import static org.keycloak.authentication.captcha.ReCaptchaEnterpriseProviderFactory.INVISIBLE;
import static org.keycloak.authentication.captcha.ReCaptchaEnterpriseProviderFactory.PROJECT_ID;
import static org.keycloak.authentication.captcha.ReCaptchaEnterpriseProviderFactory.SCORE_THRESHOLD;
import static org.keycloak.authentication.captcha.ReCaptchaEnterpriseProviderFactory.SITE_KEY;
import static org.keycloak.authentication.captcha.ReCaptchaEnterpriseProviderFactory.USE_RECAPTCHA_NET;

public class ReCaptchaEnterpriseProvider implements CaptchaProvider {

    private static final Logger logger = Logger.getLogger(ReCaptchaEnterpriseProvider.class);

    public static final String G_RECAPTCHA_RESPONSE = "g-recaptcha-response";

    private final KeycloakSession session;

    public ReCaptchaEnterpriseProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void prepareForm(LoginFormsProvider form, Map<String, String> config) {
        String languageTag = session.getContext().resolveLocale(null).toLanguageTag();
        boolean invisible = Boolean.parseBoolean(config.get(INVISIBLE));
        String action = StringUtil.isNullOrEmpty(config.get(ACTION)) ? "register" : config.get(ACTION);
        String siteKey = config.get(SITE_KEY);

        // Generic captcha attributes
        form.setAttribute(CAPTCHA_REQUIRED, true);
        form.setAttribute(CAPTCHA_SITE_KEY, siteKey);
        form.setAttribute(CAPTCHA_ACTION, action);
        form.setAttribute(CAPTCHA_WIDGET_CLASS, "g-recaptcha");
        form.setAttribute(CAPTCHA_VISIBLE, !invisible);

        // Legacy attributes for backwards compatibility
        form.setAttribute("recaptchaRequired", true);
        form.setAttribute("recaptchaSiteKey", siteKey);
        form.setAttribute("recaptchaAction", action);
        form.setAttribute("recaptchaVisible", !invisible);

        form.addScript(getScriptUrl(config, languageTag));
    }

    @Override
    public boolean validateToken(String captchaResponse, Map<String, String> config) {
        logger.trace("Requesting assessment of Google reCAPTCHA Enterprise");
        try {
            HttpPost request = buildAssessmentRequest(captchaResponse, config);
            HttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
            HttpResponse response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.errorf("Could not create reCAPTCHA assessment: %s", response.getStatusLine());
                EntityUtils.consumeQuietly(response.getEntity());
                throw new Exception(response.getStatusLine().getReasonPhrase());
            }

            RecaptchaAssessmentResponse assessment = JsonSerialization.readValue(
                    response.getEntity().getContent(), RecaptchaAssessmentResponse.class);
            logger.tracef("Got assessment response: %s", assessment);

            String tokenAction = assessment.getTokenProperties().getAction();
            String expectedAction = assessment.getEvent().getExpectedAction();
            if (!tokenAction.equals(expectedAction)) {
                logger.warnf("The action name of the reCAPTCHA token '%s' does not match the expected action '%s'!",
                        tokenAction, expectedAction);
                return false;
            }

            boolean valid = assessment.getTokenProperties().isValid();
            double score = assessment.getRiskAnalysis().getScore();
            logger.debugf("reCAPTCHA assessment: valid=%s, score=%f", valid, score);

            return valid && score >= parseDoubleFromConfig(config, SCORE_THRESHOLD);

        } catch (Exception e) {
            ServicesLogger.LOGGER.recaptchaFailed(e);
        }

        return false;
    }

    @Override
    public String getResponseFormFieldName() {
        return G_RECAPTCHA_RESPONSE;
    }

    @Override
    public boolean isConfigValid(Map<String, String> config) {
        return config != null
                && Stream.of(PROJECT_ID, SITE_KEY, API_KEY, ACTION)
                        .noneMatch(key -> StringUtil.isNullOrEmpty(config.get(key)))
                && parseDoubleFromConfig(config, SCORE_THRESHOLD) != null;
    }

    @Override
    public void close() {
    }

    private String getScriptUrl(Map<String, String> config, String languageTag) {
        return "https://www." + getRecaptchaDomain(config) + "/recaptcha/enterprise.js?hl=" + languageTag;
    }

    private String getRecaptchaDomain(Map<String, String> config) {
        return Boolean.parseBoolean(config.get(USE_RECAPTCHA_NET)) ? "recaptcha.net" : "google.com";
    }

    private HttpPost buildAssessmentRequest(String captcha, Map<String, String> config) throws IOException {
        String url = String.format("https://recaptchaenterprise.googleapis.com/v1/projects/%s/assessments?key=%s",
                config.get(PROJECT_ID), config.get(API_KEY));

        HttpPost request = new HttpPost(url);
        RecaptchaAssessmentRequest body = new RecaptchaAssessmentRequest(
                captcha, config.get(SITE_KEY), config.get(ACTION));
        request.setEntity(new StringEntity(JsonSerialization.writeValueAsString(body)));
        request.setHeader("Content-type", "application/json; charset=utf-8");

        logger.tracef("Built assessment request: %s", body);
        return request;
    }

    private Double parseDoubleFromConfig(Map<String, String> config, String key) {
        String value = config.getOrDefault(key, "");
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            logger.warnf("Could not parse config %s as double: '%s'", key, value);
        }
        return null;
    }
}
