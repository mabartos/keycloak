package org.keycloak.it.approvaltests;

import static org.keycloak.config.LoggingOptions.GELF_ENABLED;

public class NoGelfLabeler extends WindowsOrUnixOsEnvironmentLabeller {

    @Override
    public String call() {
        String osEnvironmentLabeler = super.call();
        return GELF_ENABLED ? osEnvironmentLabeler : osEnvironmentLabeler.concat(".no-gelf");
    }
}
