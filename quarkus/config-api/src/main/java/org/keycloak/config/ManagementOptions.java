package org.keycloak.config;

import org.keycloak.common.crypto.FipsMode;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ManagementOptions {

    public static final Option<Boolean> MANAGEMENT_ENABLED = new OptionBuilder<>("management-enabled", Boolean.class)
            .category(OptionCategory.MANAGEMENT)
            .description("")
            .defaultValue(Boolean.TRUE)
            .buildTime(true)
            .build();

    public static final Option<Boolean> MANAGEMENT_HOST = new OptionBuilder<>("management-host", Boolean.class)
            .category(OptionCategory.MANAGEMENT)
            .description("")
            .defaultValue(Boolean.TRUE)
            .buildTime(true)
            .build();

    public static final Option<Boolean> MANAGEMENT_PORT = new OptionBuilder<>("management-port", Boolean.class)
            .category(OptionCategory.MANAGEMENT)
            .description("")
            .defaultValue(Boolean.TRUE)
            .buildTime(true)
            .build();

    public static final Option<Boolean> MANAGEMENT_RELATIVE_PATH = new OptionBuilder<>("management-relative-path", Boolean.class)
            .category(OptionCategory.MANAGEMENT)
            .description("")
            .defaultValue(Boolean.TRUE)
            .buildTime(true)
            .build();

    // TODO ??
    public static final Option<Boolean> MANAGEMENT_AUTH_BASIC_ENABLED = new OptionBuilder<>("management-auth-basic-enabled", Boolean.class)
            .category(OptionCategory.MANAGEMENT)
            .description("")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

    // TODO ??
    public static final Option<Boolean> MANAGEMENT_AUTH_PROACTIVE_ENABLED = new OptionBuilder<>("management-auth-proactive-enabled", Boolean.class)
            .category(OptionCategory.MANAGEMENT)
            .description("")
            .defaultValue(Boolean.TRUE)
            .buildTime(true)
            .build();

    public static final Option<Boolean> MANAGEMENT_HTTPS_ENABLED = new OptionBuilder<>("management-https-enabled", Boolean.class)
            .category(OptionCategory.MANAGEMENT)
            .description("")
            .defaultValue(Boolean.TRUE)
            .buildTime(true)
            .build();

    public static final Option<HttpOptions.ClientAuth> MANAGEMENT_HTTPS_CLIENT_AUTH = new OptionBuilder<>("management-https-client-auth", HttpOptions.ClientAuth.class)
            .category(OptionCategory.MANAGEMENT)
            .description("")
            .defaultValue(HttpOptions.ClientAuth.none)
            .buildTime(true)
            .build();

    public static final Option<String> MANAGEMENT_HTTPS_CIPHER_SUITES = new OptionBuilder<>("management-https-cipher-suites", String.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The cipher suites to use for the management server. If none is given, a reasonable default is selected.")
            .build();

    public static final Option<List<String>> MANAGEMENT_HTTPS_PROTOCOLS = OptionBuilder.listOptionBuilder("management-https-protocols", String.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The list of protocols to explicitly enable for the management server.")
            .defaultValue(List.of("TLSv1.3,TLSv1.2"))
            .build();

    public static final Option<File> MANAGEMENT_HTTPS_CERTIFICATE_FILE = new OptionBuilder<>("management-https-certificate-file", File.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The file path to a server certificate or certificate chain in PEM format for the management server.")
            .build();

    public static final Option<File> MANAGEMENT_HTTPS_CERTIFICATE_KEY_FILE = new OptionBuilder<>("management-https-certificate-key-file", File.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The file path to a private key in PEM format for the management server.")
            .build();

    public static final Option<File> MANAGEMENT_HTTPS_KEY_STORE_FILE = new OptionBuilder<>("management-https-key-store-file", File.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The key store which holds the certificate information instead of specifying separate files for the management server.")
            .build();

    public static final Option<String> MANAGEMENT_HTTPS_KEY_STORE_PASSWORD = new OptionBuilder<>("management-https-key-store-password", String.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The password of the key store file for the management server.")
            .defaultValue("password")
            .build();

    public static final Option<String> MANAGEMENT_HTTPS_KEY_STORE_TYPE = new OptionBuilder<>("management-https-key-store-type", String.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The type of the key store file for the management server. " +
                    "If not given, the type is automatically detected based on the file name. " +
                    "If '" + SecurityOptions.FIPS_MODE.getKey() + "' is set to '" + FipsMode.STRICT + "' and no value is set, it defaults to 'BCFKS'.")
            .build();

    //TODO USE SYSTEM TRUSTSTORE
    /*public static final Option<File> MANAGEMENT_HTTPS_TRUST_STORE_FILE = new OptionBuilder<>("management-https-trust-store-file", File.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The trust store which holds the certificate information of the certificates to trust.")
            .deprecated("Use the System Truststore instead, see the docs for details.")
            .build();

    public static final Option<String> MANAGEMENT_HTTPS_TRUST_STORE_PASSWORD = new OptionBuilder<>("management-https-trust-store-password", String.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The password of the trust store file.")
            .deprecated("Use the System Truststore instead, see the docs for details.")
            .build();

    public static final Option<String> MANAGEMENT_HTTPS_TRUST_STORE_TYPE = new OptionBuilder<>("management-https-trust-store-type", String.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The type of the trust store file. " +
                    "If not given, the type is automatically detected based on the file name. " +
                    "If '" + SecurityOptions.FIPS_MODE.getKey() + "' is set to '" + FipsMode.STRICT + "' and no value is set, it defaults to 'BCFKS'.")
            .deprecated("Use the System Truststore instead, see the docs for details.")
            .build();*/


}
