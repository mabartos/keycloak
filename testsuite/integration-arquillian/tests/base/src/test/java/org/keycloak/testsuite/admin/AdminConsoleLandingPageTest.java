package org.keycloak.testsuite.admin;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
public class AdminConsoleLandingPageTest extends AbstractKeycloakTest {

    private CloseableHttpClient client;

    @BeforeEach
    public void before() {
        client = HttpClientBuilder.create().build();
    }

    @AfterEach
    public void after() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Test
    public void landingPage() throws IOException {
        String body = SimpleHttp.doGet(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/admin/master/console", client).asString();

        String authUrl = body.substring(body.indexOf("var authUrl = '") + 15);
        authUrl = authUrl.substring(0, authUrl.indexOf("'"));
        Assertions.assertEquals(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", authUrl);

        String resourceUrl = body.substring(body.indexOf("var resourceUrl = '") + 19);
        resourceUrl = resourceUrl.substring(0, resourceUrl.indexOf("'"));
        Assertions.assertTrue(resourceUrl.matches("/auth/resources/[^/]*/admin/([a-z]*|[a-z]*-[a-z]*)"));

        String consoleBaseUrl = body.substring(body.indexOf("var consoleBaseUrl = '") + 22);
        consoleBaseUrl = consoleBaseUrl.substring(0, consoleBaseUrl.indexOf("'"));
        Assertions.assertEquals(consoleBaseUrl, "/auth/admin/master/console/");

        Pattern p = Pattern.compile("link href=\"([^\"]*)\"");
        Matcher m = p.matcher(body);

        while(m.find()) {
                String url = m.group(1);
                Assertions.assertTrue(url.startsWith("/auth/resources/"));
        }

        p = Pattern.compile("script src=\"([^\"]*)\"");
        m = p.matcher(body);

        while(m.find()) {
            String url = m.group(1);
            if (url.contains("keycloak.js")) {
                Assertions.assertTrue(url, url.startsWith("/auth/js/"));
            } else {
                Assertions.assertTrue(url, url.startsWith("/auth/resources/"));
            }
        }
    }

}
