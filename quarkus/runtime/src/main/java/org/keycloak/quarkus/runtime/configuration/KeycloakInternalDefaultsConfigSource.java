package org.keycloak.quarkus.runtime.configuration;

import io.smallrye.config.PropertiesConfigSource;

import java.util.HashMap;
import java.util.Map;

public class KeycloakInternalDefaultsConfigSource extends PropertiesConfigSource {

    public static final String NAME = "KcInternalDefaultsConfigSource";
    public static final int ORDINAL = PersistedConfigSource.ORDINAL - 1;

    private static final Map<String, String> PROPERTIES = new HashMap<>();
    private static boolean initialized;

    public KeycloakInternalDefaultsConfigSource() {
        super(PROPERTIES, NAME, ORDINAL);
        initialized = true;
    }

    public static void addProperty(String name, String value) throws UnsupportedOperationException {
        if (initialized) {
            throw new UnsupportedOperationException("You can add properties only before initializing this config source to set some defaults during startup");
        }
        PROPERTIES.put(name, value);
    }
}
