package org.keycloak.authentication.captcha;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * Service for managing CAPTCHA provider instances stored as {@link ComponentModel}.
 *
 * <p>Wraps the {@link RealmModel} component API, providing conversion between
 * {@link CaptchaInstance} (REST representation) and {@link ComponentModel} (storage).
 * The default instance is tracked via a realm attribute.</p>
 */
public class CaptchaConfigService {

    private static final String PROVIDER_TYPE = CaptchaProvider.class.getName();

    private final KeycloakSession session;
    private final RealmModel realm;

    public CaptchaConfigService(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
    }

    // ---- CRUD ----

    /**
     * Create a new CAPTCHA provider instance.
     *
     * @return the created instance with its generated ID
     */
    public CaptchaInstance addInstance(CaptchaInstance instance) {
        ComponentModel model = toComponentModel(instance);
        model = realm.addComponentModel(model);
        return toInstance(model);
    }

    /**
     * Get a CAPTCHA instance by component ID.
     *
     * @return the instance, or {@code null} if not found or not a CAPTCHA component
     */
    public CaptchaInstance getInstance(String id) {
        ComponentModel model = realm.getComponent(id);
        if (model == null || !PROVIDER_TYPE.equals(model.getProviderType())) {
            return null;
        }
        return toInstance(model);
    }

    /**
     * List all CAPTCHA instances configured for this realm.
     */
    public Stream<CaptchaInstance> getInstances() {
        return realm.getComponentsStream(realm.getId(), PROVIDER_TYPE)
                .map(CaptchaConfigService::toInstance);
    }

    /**
     * Update an existing CAPTCHA instance.
     *
     * @return the updated instance, or {@code null} if not found
     */
    public CaptchaInstance updateInstance(String id, CaptchaInstance instance) {
        ComponentModel existing = realm.getComponent(id);
        if (existing == null || !PROVIDER_TYPE.equals(existing.getProviderType())) {
            return null;
        }

        existing.setName(instance.getName() != null ? instance.getName() : existing.getName());
        existing.setProviderId(instance.getProviderId() != null ? instance.getProviderId() : existing.getProviderId());

        if (instance.getConfig() != null) {
            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            instance.getConfig().forEach(config::putSingle);
            existing.setConfig(config);
        }

        realm.updateComponent(existing);
        return toInstance(existing);
    }

    /**
     * Remove a CAPTCHA instance. Also clears the default if it pointed to this instance.
     *
     * @return {@code true} if the instance existed and was removed
     */
    public boolean removeInstance(String id) {
        ComponentModel model = realm.getComponent(id);
        if (model == null || !PROVIDER_TYPE.equals(model.getProviderType())) {
            return false;
        }

        realm.removeComponent(model);

        // Clear default if it pointed to this instance
        if (id.equals(getDefaultInstanceId())) {
            clearDefault();
        }

        return true;
    }

    // ---- Default instance ----

    /**
     * Get the component ID of the default CAPTCHA instance, or {@code null}.
     */
    public String getDefaultInstanceId() {
        return realm.getAttribute(CaptchaConstants.CAPTCHA_DEFAULT);
    }

    /**
     * Load the default CAPTCHA instance, or {@code null} if not set.
     */
    public CaptchaInstance getDefaultInstance() {
        String id = getDefaultInstanceId();
        if (id == null) {
            return null;
        }
        return getInstance(id);
    }

    /**
     * Set the default CAPTCHA instance by component ID.
     *
     * @throws IllegalArgumentException if the instance does not exist
     */
    public void setDefaultInstanceId(String id) {
        CaptchaInstance instance = getInstance(id);
        if (instance == null) {
            throw new IllegalArgumentException("CAPTCHA instance not found: " + id);
        }
        realm.setAttribute(CaptchaConstants.CAPTCHA_DEFAULT, id);
    }

    /**
     * Clear the default CAPTCHA instance.
     */
    public void clearDefault() {
        realm.removeAttribute(CaptchaConstants.CAPTCHA_DEFAULT);
    }

    // ---- Validation ----

    /**
     * Validate that the instance references a known CAPTCHA provider factory.
     *
     * @throws IllegalArgumentException if providerId is missing or unknown
     */
    public void validateInstance(CaptchaInstance instance) {
        if (instance.getProviderId() == null || instance.getProviderId().isEmpty()) {
            throw new IllegalArgumentException("providerId is required");
        }

        if (session.getKeycloakSessionFactory()
                .getProviderFactory(CaptchaProvider.class, instance.getProviderId()) == null) {
            throw new IllegalArgumentException("Unknown CAPTCHA provider: " + instance.getProviderId());
        }
    }

    // ---- Conversion ----

    private ComponentModel toComponentModel(CaptchaInstance instance) {
        ComponentModel model = new ComponentModel();
        model.setName(instance.getName());
        model.setProviderId(instance.getProviderId());
        model.setProviderType(PROVIDER_TYPE);
        model.setParentId(realm.getId());

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        if (instance.getConfig() != null) {
            instance.getConfig().forEach(config::putSingle);
        }
        model.setConfig(config);

        return model;
    }

    static CaptchaInstance toInstance(ComponentModel model) {
        Map<String, String> config = new HashMap<>();
        model.getConfig().forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                config.put(key, values.get(0));
            }
        });

        return new CaptchaInstance(model.getId(), model.getName(), model.getProviderId(), config);
    }
}
