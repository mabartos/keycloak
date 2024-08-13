package org.keycloak.connections.httpclient;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;

public class DefaultHttpClientProvider implements HttpClientProvider {
    private static final Logger logger = Logger.getLogger(DefaultHttpClientProvider.class);

    protected final CloseableHttpClient httpClient;
    protected final AbstractResponseHandler<InputStream> responseHandler;
    protected final BasicResponseHandler stringResponseHandler;
    protected long maxConsumedResponseSize;

    public DefaultHttpClientProvider(CloseableHttpClient httpClient,
                                     AbstractResponseHandler<InputStream> responseHandler,
                                     BasicResponseHandler stringResponseHandler,
                                     long maxConsumedResponseSize) {
        this.httpClient = httpClient;
        this.responseHandler = responseHandler;
        this.stringResponseHandler = stringResponseHandler;
        this.maxConsumedResponseSize = maxConsumedResponseSize;
    }

    @Override
    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public void close() {

    }

    @Override
    public int postText(String uri, String text) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setEntity(EntityBuilder.create().setText(text).setContentType(ContentType.TEXT_PLAIN).build());
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            try {
                return response.getStatusLine().getStatusCode();
            } finally {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
            throw t;
        }
    }

    @Override
    public String getString(String uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        HttpResponse response = httpClient.execute(request);
        String body = stringResponseHandler.handleResponse(response);
        if (body == null) {
            throw new IOException("No content returned from HTTP call");
        }
        return body;
    }

    @Override
    public InputStream getInputStream(String uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        HttpResponse response = httpClient.execute(request);
        InputStream body = responseHandler.handleResponse(response);
        if (body == null) {
            throw new IOException("No content returned from HTTP call");
        }
        return body;
    }

    @Override
    public long getMaxConsumedResponseSize() {
        return maxConsumedResponseSize;
    }
}
