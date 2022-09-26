package org.keycloak.testsuite.util;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.keycloak.testsuite.arquillian.undertow.lb.SimpleUndertowLoadBalancer;

import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.getHttpAuthServerContextRoot;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.util.ServerURLs.removeDefaultPorts;

public class ReverseProxy implements BeforeAllCallback {

    public static String DEFAULT_PROXY_HOST = "proxy.kc.127.0.0.1.nip.io";
    public static final int DEFAULT_HTTP_PORT = 8666;
    public static final int DEFAULT_HTTPS_PORT = 8667;

    private final SimpleUndertowLoadBalancer proxy;

    public ReverseProxy() {
        this(DEFAULT_PROXY_HOST);
    }
    
    public ReverseProxy(String host) {
        this(host, "node1=" + getHttpAuthServerContextRoot() + "/auth");
    }

    public ReverseProxy(String host, String nodes) {
        proxy = new SimpleUndertowLoadBalancer(host, DEFAULT_HTTP_PORT, DEFAULT_HTTPS_PORT, nodes);
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        try {
            proxy.start();
            proxy.enableAllBackendNodes();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        } finally {
            proxy.stop();
        }
    }

    public String getUrl() {
        String scheme = AUTH_SERVER_SSL_REQUIRED ? "https" : "http";
        int port = AUTH_SERVER_SSL_REQUIRED ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT;
        return removeDefaultPorts(String.format("%s://%s:%s", scheme, DEFAULT_PROXY_HOST, port));
    }
}
