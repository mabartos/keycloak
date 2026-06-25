package org.keycloak.representations.idm;

import java.util.Map;

public class SecretRotationConfigRepresentation {

    private String source;
    private Map<String, Object> configuration;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
}
