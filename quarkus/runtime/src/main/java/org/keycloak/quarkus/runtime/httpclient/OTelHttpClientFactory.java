package org.keycloak.quarkus.runtime.httpclient;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.apachehttpclient.v4_3.ApacheHttpClientTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import org.keycloak.connections.httpclient.DefaultHttpClientFactory;
import org.keycloak.connections.httpclient.DefaultHttpClientProvider;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

public class OTelHttpClientFactory extends DefaultHttpClientFactory {

    private static final OpenTelemetry openTelemetry = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();

    @Override
    public HttpClientProvider create(KeycloakSession session) {
        var client = super.create(session);
        var httpClientTelemetry = ApacheHttpClientTelemetry.builder(openTelemetry).build().;
        return new DefaultHttpClientProvider(httpClientTelemetry,client.);
    }
}
