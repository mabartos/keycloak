package org.keycloak.quarkus.runtime.configuration;

import io.quarkus.bootstrap.logging.QuarkusDelayedHandler;
import org.jboss.logmanager.ExtLogRecord;

public class KcDelayedLogHandler extends QuarkusDelayedHandler {

    @Override
    protected void doPublish(final ExtLogRecord record) {
        super.doPublish(record);
        System.err.println("HERE");
    }
}
