package org.keycloak.quarkus.runtime.configuration.mappers;

import org.keycloak.config.ProxyOptions;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class ProxyPropertyMappers {

    private ProxyPropertyMappers(){}

    public static PropertyMapper[] getProxyPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(ProxyOptions.PROXY)
                        .to("quarkus.http.proxy.proxy-address-forwarding")
                        .paramLabel("mode")
                        .build(),
                fromOption(ProxyOptions.PROXY_PARSE_HEADER)
                        .paramLabel("mode")
                        .build(),
                fromOption(ProxyOptions.PROXY_FORWARDED_HOST)
                        .to("quarkus.http.proxy.enable-forwarded-host")
                        .mapFrom("proxy-parse-headers")
                        .build(),
                fromOption(ProxyOptions.PROXY_FORWARDED_HEADER_ENABLED)
                        .to("quarkus.http.proxy.allow-forwarded")
                        .mapFrom("proxy-parse-headers")
                        .build(),
                fromOption(ProxyOptions.PROXY_X_FORWARDED_HEADER_ENABLED)
                        .to("quarkus.http.proxy.allow-x-forwarded")
                        .mapFrom("proxy-parse-headers")
                        .build()
        };
    }
}
