package org.keycloak.it.tracing;

import org.keycloak.it.TestProvider;

import java.util.Collections;
import java.util.Map;

public class CustomSamplerTestProvider implements TestProvider {
    @Override
    public Class[] getClasses() {
        return new Class[]{CustomSamplerProvider.class, CustomSamplerProvider.CustomSPISampler.class};
    }

    @Override
    public Map<String, String> getManifestResources() {
        return Collections.singletonMap("io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider", "services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider");
    }
}
