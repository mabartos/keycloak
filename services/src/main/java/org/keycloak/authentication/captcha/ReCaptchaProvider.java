package org.keycloak.authentication.captcha;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
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
import static org.keycloak.authentication.captcha.ReCaptchaProviderFactory.ACTION;
import static org.keycloak.authentication.captcha.ReCaptchaProviderFactory.INVISIBLE;
import static org.keycloak.authentication.captcha.ReCaptchaProviderFactory.OLD_SECRET;
import static org.keycloak.authentication.captcha.ReCaptchaProviderFactory.SECRET_KEY;
import static org.keycloak.authentication.captcha.ReCaptchaProviderFactory.SITE_KEY;
import static org.keycloak.authentication.captcha.ReCaptchaProviderFactory.USE_RECAPTCHA_NET;

public class ReCaptchaProvider implements CaptchaProvider {

    private static final Logger logger = Logger.getLogger(ReCaptchaProvider.class);

    public static final String G_RECAPTCHA_RESPONSE = "g-recaptcha-response";

    private final KeycloakSession session;

    public ReCaptchaProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void prepareForm(LoginFormsProvider form, Map<String, String> config) {
        String languageTag = session.getContext().resolveLocale(null).toLanguageTag();
        boolean invisible = Boolean.parseBoolean(config.get(INVISIBLE));
        String action = StringUtil.isNullOrEmpty(config.get(ACTION)) ? "register" : config.get(ACTION);
        String siteKey = config.get(SITE_KEY);

        // Generic captcha attributes (for captcha-commons.ftl)
        form.setAttribute(CAPTCHA_REQUIRED, true);
        form.setAttribute(CAPTCHA_SITE_KEY, siteKey);
        form.setAttribute(CAPTCHA_ACTION, action);
        form.setAttribute(CAPTCHA_WIDGET_CLASS, "g-recaptcha");
        form.setAttribute(CAPTCHA_VISIBLE, !invisible);

        // Legacy attributes (for backwards compatibility with custom themes)
        form.setAttribute("recaptchaRequired", true);
        form.setAttribute("recaptchaSiteKey", siteKey);
        form.setAttribute("recaptchaAction", action);
        form.setAttribute("recaptchaVisible", !invisible);

        form.addScript(getScriptUrl(config, languageTag));
    }

    @Override
    public boolean validateToken(String captchaResponse, Map<String, String> config) {
        logger.trace("Verifying reCAPTCHA using non-enterprise API");
        CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();

        HttpPost post = new HttpPost("https://www." + getRecaptchaDomain(config) + "/recaptcha/api/siteverify");
        List<NameValuePair> formparams = new LinkedList<>();
        String secret = resolveSecret(config);
        formparams.add(new BasicNameValuePair("secret", secret));
        formparams.add(new BasicNameValuePair("response", captchaResponse));
        String remoteAddr = session.getContext().getConnection().getRemoteAddr();
        if (remoteAddr != null) {
            formparams.add(new BasicNameValuePair("remoteip", remoteAddr));
        }

        try {
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, StandardCharsets.UTF_8);
            post.setEntity(form);
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                InputStream content = response.getEntity().getContent();
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> json = JsonSerialization.readValue(content, Map.class);
                    return Boolean.TRUE.equals(json.get("success"));
                } finally {
                    EntityUtils.consumeQuietly(response.getEntity());
                }
            }
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
                && !StringUtil.isNullOrEmpty(config.get(SITE_KEY))
                && (!StringUtil.isNullOrEmpty(config.get(SECRET_KEY)) || !StringUtil.isNullOrEmpty(config.get(OLD_SECRET)));
    }

    @Override
    public void close() {
    }

    private String getScriptUrl(Map<String, String> config, String languageTag) {
        return "https://www." + getRecaptchaDomain(config) + "/recaptcha/api.js?hl=" + languageTag;
    }

    private String getRecaptchaDomain(Map<String, String> config) {
        return Boolean.parseBoolean(config.get(USE_RECAPTCHA_NET)) ? "recaptcha.net" : "google.com";
    }

    private String resolveSecret(Map<String, String> config) {
        String secret = config.get(SECRET_KEY);
        if (StringUtil.isNullOrEmpty(secret)) {
            secret = config.get(OLD_SECRET);
            if (!StringUtil.isNullOrEmpty(secret)) {
                config.put(SECRET_KEY, secret);
                config.remove(OLD_SECRET);
            }
        }
        return secret;
    }
}
