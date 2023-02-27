package org.keycloak.config;

public class ExportOptions {

    public static final Option<String> FILE = new OptionBuilder<>("file", String.class)
            .category(OptionCategory.GENERAL)
            .buildTime(false)
            .build();

    public static final Option<String> REALM = new OptionBuilder<>("realm", String.class)
            .category(OptionCategory.GENERAL)
            .buildTime(false)
            .build();

    public static final Option<String> DIR = new OptionBuilder<>("dir", String.class)
            .category(OptionCategory.GENERAL)
            .buildTime(false)
            .build();

}
