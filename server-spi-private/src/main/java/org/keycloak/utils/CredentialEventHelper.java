package org.keycloak.utils;

import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;

public class CredentialEventHelper {

    // Update
    public static EventBuilder update(EventBuilder eventBuilder) {
        return update(eventBuilder, "");
    }

    public static EventBuilder update(EventBuilder eventBuilder, String credentialType) {
        return perform(eventBuilder, EventType.UPDATE_CREDENTIAL, credentialType);
    }

    public static EventBuilder updateError(EventBuilder eventBuilder) {
        return updateError(eventBuilder, "");
    }

    public static EventBuilder updateError(EventBuilder eventBuilder, String credentialType) {
        return perform(eventBuilder, EventType.UPDATE_CREDENTIAL_ERROR, credentialType);
    }

    // Remove

    public static EventBuilder remove(EventBuilder eventBuilder) {
        return remove(eventBuilder, "");
    }

    public static EventBuilder remove(EventBuilder eventBuilder, String credentialType) {
        return perform(eventBuilder, EventType.REMOVE_CREDENTIAL, credentialType);
    }

    public static EventBuilder removeError(EventBuilder eventBuilder) {
        return removeError(eventBuilder, "");
    }

    public static EventBuilder removeError(EventBuilder eventBuilder, String credentialType) {
        return perform(eventBuilder, EventType.REMOVE_CREDENTIAL_ERROR, credentialType);
    }

    // Register

    public static EventBuilder register(EventBuilder eventBuilder) {
        return register(eventBuilder, "");
    }

    public static EventBuilder register(EventBuilder eventBuilder, String credentialType) {
        return perform(eventBuilder, EventType.REGISTER_CREDENTIAL, credentialType);
    }

    public static EventBuilder registerError(EventBuilder eventBuilder) {
        return registerError(eventBuilder, "");
    }

    public static EventBuilder registerError(EventBuilder eventBuilder, String credentialType) {
        return perform(eventBuilder, EventType.REGISTER_CREDENTIAL_ERROR, credentialType);
    }

    public static EventBuilder perform(EventBuilder eventBuilder, EventType eventType) {
        return perform(eventBuilder, eventType, "");
    }

    public static EventBuilder perform(EventBuilder eventBuilder, EventType eventType, String credentialType) {
        eventBuilder.event(eventType);
        if (StringUtil.isNotBlank(credentialType)) {
            eventBuilder.detail(Details.CREDENTIAL_TYPE, credentialType);
        }
        return eventBuilder;
    }
}
