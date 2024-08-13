package org.keycloak.quarkus.runtime.httpclient;

import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.connections.httpclient.DefaultHttpClientProvider;

import java.io.InputStream;

public class OTelHttpClientProvider extends DefaultHttpClientProvider {
    public OTelHttpClientProvider(CloseableHttpClient httpClient, AbstractResponseHandler<InputStream> responseHandler, BasicResponseHandler stringResponseHandler, long maxConsumedResponseSize) {
        super(httpClient, responseHandler, stringResponseHandler, maxConsumedResponseSize);
        this.httpClient =
    }


}
