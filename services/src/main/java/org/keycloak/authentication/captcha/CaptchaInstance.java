package org.keycloak.authentication.captcha;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a configured CAPTCHA provider instance.
 *
 * <p>Used as the REST representation for CAPTCHA configuration.
 * Internally, instances are stored as {@link org.keycloak.component.ComponentModel}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaptchaInstance {

    private String id;
    private String name;
    private String providerId;
    private Map<String, String> config;

    public CaptchaInstance() {
    }

    public CaptchaInstance(String id, String name, String providerId, Map<String, String> config) {
        this.id = id;
        this.name = name;
        this.providerId = providerId;
        this.config = config;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Map<String, String> getConfig() {
        if (config == null) {
            config = new HashMap<>();
        }
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
}
