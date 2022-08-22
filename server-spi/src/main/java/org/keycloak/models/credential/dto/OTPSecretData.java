package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OTPSecretData {
    private final String value;
    private long lastValidInterval;

    @JsonCreator
    public OTPSecretData(@JsonProperty("value") String value,
                         @JsonProperty("validationInterval") long validationInterval) {
        this.value = value;
        this.lastValidInterval = validationInterval;
    }

    public OTPSecretData(String value) {
        this.value = value;
        this.lastValidInterval = -1L;
    }

    public String getValue() {
        return value;
    }

    public long getLastValidInterval() {
        return lastValidInterval;
    }

    public void setLastValidInterval(long lastValidInterval) {
        this.lastValidInterval = lastValidInterval;
    }
}
