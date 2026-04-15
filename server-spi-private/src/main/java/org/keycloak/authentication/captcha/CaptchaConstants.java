package org.keycloak.authentication.captcha;

/**
 * Constants for CAPTCHA form attributes used by built-in providers and templates.
 */
public final class CaptchaConstants {

    // Realm attribute name for the default CAPTCHA instance (stores component ID)
    public static final String CAPTCHA_DEFAULT = "captcha.default";

    // Form attribute names
    public static final String CAPTCHA_REQUIRED = "captchaRequired";
    public static final String CAPTCHA_SITE_KEY = "captchaSiteKey";
    public static final String CAPTCHA_ACTION = "captchaAction";
    public static final String CAPTCHA_WIDGET_CLASS = "captchaWidgetClass";
    public static final String CAPTCHA_VISIBLE = "captchaVisible";

    // Action names for different flows
    public static final String ACTION_LOGIN = "login";
    public static final String ACTION_REGISTER = "register";
    public static final String ACTION_RESET = "reset";

    private CaptchaConstants() {
    }
}
