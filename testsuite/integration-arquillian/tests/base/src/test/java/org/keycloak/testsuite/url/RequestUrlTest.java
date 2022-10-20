package org.keycloak.testsuite.url;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.jsoup.helper.HttpConnection;
import org.junit.Test;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;


public class RequestUrlTest extends AbstractKeycloakTest {


    @Test
    public void absoluteUriRequestPath() throws IOException {
        try (CloseableHttpClient httpClient = (CloseableHttpClient) new HttpClientBuilder().build()) {
            HttpUriRequest request = RequestBuilder.create("GET")
                    .addHeader("Host", "localhost:8543")
                    .setUri("https://localhost:8543/auth/realms/master/.well-known/openid-configuration")
                    .build();

            System.out.println(request.toString());
            System.out.println(request.getRequestLine().toString());


            try (CloseableHttpResponse response = httpClient.execute(request)) {
                System.out.println(response.getStatusLine().getStatusCode());
                System.out.println(EntityUtils.toString(response.getEntity()));
                assertThat(response.getStatusLine().getStatusCode(), CoreMatchers.is(200));
            }
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

    }
}
