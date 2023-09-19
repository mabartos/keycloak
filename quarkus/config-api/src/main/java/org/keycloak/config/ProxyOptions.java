package org.keycloak.config;

public class ProxyOptions {
    
    public static final Option<Boolean> PROXY = new OptionBuilder<>("proxy", Boolean.class)
            .category(OptionCategory.PROXY)
            .description("If the server is behind a reverse proxy.")
            .defaultValue(Boolean.FALSE)
            .build();

    public static final Option<Boolean> PROXY_PARSE_HEADER = new OptionBuilder<>("proxy-parse-headers", Boolean.class)
            .category(OptionCategory.PROXY)
            .description("If the server should parse forwarded headers")
            .defaultValue(Boolean.FALSE)
            .build();

    public static final Option<Boolean> PROXY_FORWARDED_HOST = new OptionBuilder<>("proxy-forwarded-host", Boolean.class)
            .category(OptionCategory.PROXY)
            .defaultValue(Boolean.FALSE)
            .build();

    public static final Option<Boolean> PROXY_FORWARDED_HEADER_ENABLED = new OptionBuilder<>("proxy-allow-forwarded-header", Boolean.class)
            .category(OptionCategory.PROXY)
            .defaultValue(Boolean.FALSE)
            .build();

    public static final Option<Boolean> PROXY_X_FORWARDED_HEADER_ENABLED = new OptionBuilder<>("proxy-allow-x-forwarded-header", Boolean.class)
            .category(OptionCategory.PROXY)
            .defaultValue(Boolean.FALSE)
            .build();
}
