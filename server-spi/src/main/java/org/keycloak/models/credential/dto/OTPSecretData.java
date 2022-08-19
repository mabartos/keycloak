package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OTPSecretData {
    private final String value;
    private String lastValue;

    @JsonCreator
    public OTPSecretData(@JsonProperty("value") String value,
                         @JsonProperty("lastValue") String lastValue) {
        this.value = value;
        this.lastValue = lastValue;
    }

    public OTPSecretData(String value) {
        this.value = value;
        this.lastValue = "";
    }

    public String getValue() {
        return value;
    }

    public String getLastValue() {
        return lastValue;
    }

    public void setLastValue(String lastValue) {
        this.lastValue = lastValue;
    }
}
