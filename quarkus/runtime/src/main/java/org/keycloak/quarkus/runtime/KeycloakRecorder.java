/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.quarkus.runtime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import liquibase.Scope;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.infinispan.manager.DefaultCacheManager;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticatorSpi;
import org.keycloak.authentication.authenticators.browser.DeployedScriptAuthenticatorFactory;
import org.keycloak.authorization.policy.provider.PolicySpi;
import org.keycloak.authorization.policy.provider.js.DeployedScriptPolicyFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.crypto.FipsMode;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.config.TruststoreOptions;
import org.keycloak.connections.jpa.DefaultJpaConnectionProviderFactory;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionSpi;
import org.keycloak.connections.jpa.updater.liquibase.LiquibaseJpaUpdaterProviderFactory;
import org.keycloak.connections.jpa.updater.liquibase.conn.DefaultLiquibaseConnectionProvider;
import org.keycloak.policy.BlacklistPasswordPolicyProviderFactory;
import org.keycloak.protocol.ProtocolMapperSpi;
import org.keycloak.protocol.oidc.mappers.DeployedScriptOIDCProtocolMapper;
import org.keycloak.protocol.saml.mappers.DeployedScriptSAMLProtocolMapper;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderManager;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;
import org.keycloak.quarkus.runtime.storage.database.jpa.NamedJpaConnectionProviderFactory;
import org.keycloak.quarkus.runtime.storage.database.liquibase.FastServiceLocator;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.quarkus.runtime.storage.legacy.infinispan.CacheManagerFactory;
import org.keycloak.quarkus.runtime.themes.FlatClasspathThemeResourceProviderFactory;
import org.keycloak.representations.provider.ScriptProviderDescriptor;
import org.keycloak.representations.provider.ScriptProviderMetadata;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.services.ServicesLogger;
import org.keycloak.theme.ClasspathThemeProviderFactory;
import org.keycloak.theme.ClasspathThemeResourceProviderFactory;
import org.keycloak.theme.FolderThemeProviderFactory;
import org.keycloak.theme.JarThemeProviderFactory;
import org.keycloak.theme.ThemeResourceSpi;
import org.keycloak.transaction.JBossJtaTransactionManagerLookup;
import org.keycloak.truststore.TruststoreBuilder;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import liquibase.servicelocator.ServiceLocator;
import org.keycloak.url.DefaultHostnameProviderFactory;
import org.keycloak.url.FixedHostnameProviderFactory;
import org.keycloak.url.RequestHostnameProviderFactory;
import org.keycloak.userprofile.DeclarativeUserProfileProviderFactory;
import org.keycloak.util.JsonSerialization;
import org.keycloak.vault.FilesKeystoreVaultProviderFactory;
import org.keycloak.vault.FilesPlainTextVaultProviderFactory;

import static org.keycloak.quarkus.runtime.Providers.getProviderManager;
import static org.keycloak.representations.provider.ScriptProviderDescriptor.AUTHENTICATORS;
import static org.keycloak.representations.provider.ScriptProviderDescriptor.MAPPERS;
import static org.keycloak.representations.provider.ScriptProviderDescriptor.POLICIES;
import static org.keycloak.representations.provider.ScriptProviderDescriptor.SAML_MAPPERS;

@Recorder
public class KeycloakRecorder {

    public static final String DEFAULT_HEALTH_ENDPOINT = "/health";
    public static final String DEFAULT_METRICS_ENDPOINT = "/metrics";

    public void initConfig() {
        Config.init(new MicroProfileConfigProvider());
    }

    public void configureProfile(Profile.ProfileName profileName, Map<Profile.Feature, Boolean> features) {
        Profile.init(profileName, features);
    }

    public void configureTruststore() {
        String[] truststores = Configuration.getOptionalKcValue(TruststoreOptions.TRUSTSTORE_PATHS.getKey())
                .map(s -> s.split(",")).orElse(new String[0]);

        String dataDir = Environment.getDataDir();

        File truststoresDir = Optional.ofNullable(Environment.getHomePath()).map(path -> path.resolve("conf").resolve("truststores").toFile()).orElse(null);

        if (truststoresDir != null && truststoresDir.exists() && Optional.ofNullable(truststoresDir.list()).map(a -> a.length).orElse(0) > 0) {
            truststores = Stream.concat(Stream.of(truststoresDir.getAbsolutePath()), Stream.of(truststores)).toArray(String[]::new);
        } else if (truststores.length == 0) {
            return; // nothing to configure, we'll just use the system default
        }

        TruststoreBuilder.setSystemTruststore(truststores, true, dataDir);
    }

    public void configureLiquibase(Map<String, List<String>> services) {
        ServiceLocator locator = Scope.getCurrentScope().getServiceLocator();
        if (locator instanceof FastServiceLocator)
            ((FastServiceLocator) locator).initServices(services);
    }

    public void configSessionFactory(
            List<ParsedPersistenceXmlDescriptor> parsedPersistenceXmlDescriptors,
            List<ClasspathThemeProviderFactory.ThemesRepresentation> themes, boolean reaugmented) {
        KeycloakSessionFactoryWrapper factoryWrapper = configureKeycloakSessionFactory(parsedPersistenceXmlDescriptors).getValue();
        QuarkusKeycloakSessionFactory.setInstance(new QuarkusKeycloakSessionFactory(factoryWrapper.factories, factoryWrapper.defaultProviders, factoryWrapper.preConfiguredProviders, themes, reaugmented));
    }

    public RuntimeValue<CacheManagerFactory> createCacheInitializer(String config, boolean metricsEnabled, ShutdownContext shutdownContext) {
        try {
            CacheManagerFactory cacheManagerFactory = new CacheManagerFactory(config, metricsEnabled);

            shutdownContext.addShutdownTask(new Runnable() {
                @Override
                public void run() {
                    DefaultCacheManager cacheManager = cacheManagerFactory.getOrCreate();

                    if (cacheManager != null) {
                        cacheManager.stop();
                    }
                }
            });

            return new RuntimeValue<>(cacheManagerFactory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setDefaultUserProfileConfiguration(UPConfig configuration) {
        DeclarativeUserProfileProviderFactory.setDefaultConfig(configuration);
    }

    public void registerShutdownHook(ShutdownContext shutdownContext) {
        shutdownContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                QuarkusKeycloakSessionFactory.getInstance().close();
            }
        });
    }

    public HibernateOrmIntegrationRuntimeInitListener createUserDefinedUnitListener(String name) {
        return new HibernateOrmIntegrationRuntimeInitListener() {
            @Override
            public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
                InstanceHandle<AgroalDataSource> instance = Arc.container().instance(
                        AgroalDataSource.class, new DataSource() {
                            @Override public Class<? extends Annotation> annotationType() {
                                return DataSource.class;
                            }

                            @Override public String value() {
                                return name;
                            }
                        });
                propertyCollector.accept(AvailableSettings.DATASOURCE, instance.get());
            }
        };
    }

    public HibernateOrmIntegrationRuntimeInitListener createDefaultUnitListener() {
        return new HibernateOrmIntegrationRuntimeInitListener() {
            @Override
            public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
                propertyCollector.accept(AvailableSettings.DEFAULT_SCHEMA, Configuration.getRawValue("kc.db-schema"));
            }
        };
    }

    public void setCryptoProvider(FipsMode fipsMode) {
        String cryptoProvider = fipsMode.getProviderClassName();

        try {
            CryptoIntegration.setProvider(
                    (CryptoProvider) Thread.currentThread().getContextClassLoader().loadClass(cryptoProvider).getDeclaredConstructor().newInstance());
        } catch (ClassNotFoundException | NoClassDefFoundError cause) {
            if (fipsMode.isFipsEnabled()) {
                throw new RuntimeException("Failed to configure FIPS. Make sure you have added the Bouncy Castle FIPS dependencies to the 'providers' directory.");
            }
            throw new RuntimeException("Unexpected error when configuring the crypto provider: " + cryptoProvider, cause);
        } catch (Exception cause) {
            throw new RuntimeException("Unexpected error when configuring the crypto provider: " + cryptoProvider, cause);
        }
    }

    private static final Logger logger = Logger.getLogger(KeycloakRecorder.class);

    private static final List<Class<? extends ProviderFactory>> IGNORED_PROVIDER_FACTORY = List.of(
            JBossJtaTransactionManagerLookup.class,
            DefaultJpaConnectionProviderFactory.class,
            DefaultLiquibaseConnectionProvider.class,
            FolderThemeProviderFactory.class,
            LiquibaseJpaUpdaterProviderFactory.class,
            DefaultHostnameProviderFactory.class,
            FixedHostnameProviderFactory.class,
            RequestHostnameProviderFactory.class,
            FilesKeystoreVaultProviderFactory.class,
            FilesPlainTextVaultProviderFactory.class,
            BlacklistPasswordPolicyProviderFactory.class,
            ClasspathThemeResourceProviderFactory.class,
            JarThemeProviderFactory.class);

    private static final String JAR_FILE_SEPARATOR = "!/";
    private static final Map<String, Function<ScriptProviderMetadata, ProviderFactory>> DEPLOYEABLE_SCRIPT_PROVIDERS = new HashMap<>();
    private static final String KEYCLOAK_SCRIPTS_JSON_PATH = "META-INF/keycloak-scripts.json";

    static {
        DEPLOYEABLE_SCRIPT_PROVIDERS.put(AUTHENTICATORS, KeycloakRecorder::registerScriptAuthenticator);
        DEPLOYEABLE_SCRIPT_PROVIDERS.put(POLICIES, KeycloakRecorder::registerScriptPolicy);
        DEPLOYEABLE_SCRIPT_PROVIDERS.put(MAPPERS, KeycloakRecorder::registerScriptMapper);
        DEPLOYEABLE_SCRIPT_PROVIDERS.put(SAML_MAPPERS, KeycloakRecorder::registerSAMLScriptMapper);
    }

    private static ProviderFactory registerScriptAuthenticator(ScriptProviderMetadata metadata) {
        return new DeployedScriptAuthenticatorFactory(metadata);
    }

    private static ProviderFactory registerScriptPolicy(ScriptProviderMetadata metadata) {
        return new DeployedScriptPolicyFactory(metadata);
    }

    private static ProviderFactory registerScriptMapper(ScriptProviderMetadata metadata) {
        return new DeployedScriptOIDCProtocolMapper(metadata);
    }

    private static ProviderFactory registerSAMLScriptMapper(ScriptProviderMetadata metadata) {
        return new DeployedScriptSAMLProtocolMapper(metadata);
    }

    public Map<Spi, Map<Class<? extends Provider>, Map<String, ProviderFactory>>> loadFactories(
            Map<String, ProviderFactory> preConfiguredProviders) {
        Config.init(new MicroProfileConfigProvider());
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ProviderManager pm = getProviderManager(classLoader);
        Map<Spi, Map<Class<? extends Provider>, Map<String, ProviderFactory>>> factories = new HashMap<>();

        for (Spi spi : pm.loadSpis()) {
            Map<Class<? extends Provider>, Map<String, ProviderFactory>> providers = new HashMap<>();
            List<ProviderFactory> loadedFactories = new ArrayList<>();
            String provider = Config.getProvider(spi.getName());

            if (provider == null) {
                loadedFactories.addAll(pm.load(spi));
            } else {
                ProviderFactory factory = pm.load(spi, provider);

                if (factory != null) {
                    loadedFactories.add(factory);
                }
            }

            Map<String, ProviderFactory<?>> deployedScriptProviders = loadDeployedScriptProviders(classLoader, spi);

            loadedFactories.addAll(deployedScriptProviders.values());
            preConfiguredProviders.putAll(deployedScriptProviders);

            for (ProviderFactory<?> factory : loadedFactories) {
                if (IGNORED_PROVIDER_FACTORY.contains(factory.getClass())) {
                    continue;
                }

                Config.Scope scope = Config.scope(spi.getName(), factory.getId());

                if (isEnabled(factory, scope)) {
                    if (spi.isInternal() && !isInternal(factory)) {
                        ServicesLogger.LOGGER.spiMayChange(factory.getId(), factory.getClass().getName(), spi.getName());
                    }

                    providers.computeIfAbsent(spi.getProviderClass(), aClass -> new HashMap<>()).put(factory.getId(),
                            factory);
                } else {
                    logger.debugv("SPI {0} provider {1} disabled", spi.getName(), factory.getId());
                }
            }

            factories.put(spi, providers);
        }

        return factories;
    }

    private Map<String, ProviderFactory<?>> loadDeployedScriptProviders(ClassLoader classLoader, Spi spi) {
        Map<String, ProviderFactory<?>> providers = new HashMap<>();

        if (supportsDeployeableScripts(spi)) {
            try {
                Enumeration<URL> descriptorsUrls = classLoader.getResources(KEYCLOAK_SCRIPTS_JSON_PATH);

                while (descriptorsUrls.hasMoreElements()) {
                    URL url = descriptorsUrls.nextElement();
                    List<ScriptProviderDescriptor> descriptors = getScriptProviderDescriptorsFromJarFile(url);

                    if (!Environment.isDistribution()) {
                        // script providers are only loaded from classpath when running embedded
                        descriptors = new ArrayList<>(descriptors);
                        descriptors.addAll(getScriptProviderDescriptorsFromClassPath(url));
                    }

                    for (ScriptProviderDescriptor descriptor : descriptors) {
                        for (Map.Entry<String, List<ScriptProviderMetadata>> entry : descriptor.getProviders().entrySet()) {
                            if (isScriptForSpi(spi, entry.getKey())) {
                                for (ScriptProviderMetadata metadata : entry.getValue()) {
                                    ProviderFactory<?> factory = DEPLOYEABLE_SCRIPT_PROVIDERS.get(entry.getKey()).apply(metadata);
                                    providers.put(metadata.getId(), factory);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to discover script providers", e);
            }
        }

        return providers;
    }

    private List<ScriptProviderDescriptor> getScriptProviderDescriptorsFromClassPath(URL url) throws IOException {
        String file = url.getFile();

        if (!file.endsWith(".json")) {
            return List.of();
        }

        List<ScriptProviderDescriptor> descriptors = new ArrayList<>();

        try (InputStream is = url.openStream()) {
            ScriptProviderDescriptor descriptor = JsonSerialization.readValue(is, ScriptProviderDescriptor.class);

            configureScriptDescriptor(descriptor, fileName -> {
                // descriptor is at META-INF/
                Path basePath = Path.of(url.getPath()).getParent().getParent();

                try {
                    return basePath.resolve(fileName).toUri().toURL().openStream();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read script file from: " + fileName);
                }
            });
            descriptors.add(descriptor);
        }

        return descriptors;
    }

    private List<ScriptProviderDescriptor> getScriptProviderDescriptorsFromJarFile(URL url) throws IOException {
        String file = url.getFile();

        if (!file.contains(JAR_FILE_SEPARATOR)) {
            return List.of();
        }

        List<ScriptProviderDescriptor> descriptors = new ArrayList<>();

        try (JarFile jarFile = new JarFile(file.substring("file:".length(), file.indexOf(JAR_FILE_SEPARATOR)))) {
            JarEntry descriptorEntry = jarFile.getJarEntry(KEYCLOAK_SCRIPTS_JSON_PATH);

            try (InputStream is = jarFile.getInputStream(descriptorEntry)) {
                ScriptProviderDescriptor descriptor = JsonSerialization.readValue(is, ScriptProviderDescriptor.class);

                configureScriptDescriptor(descriptor, fileName -> {
                    try {
                        JarEntry scriptFile = jarFile.getJarEntry(fileName);
                        return jarFile.getInputStream(scriptFile);
                    } catch (IOException cause) {
                        throw new RuntimeException("Failed to read script file from file: " + fileName, cause);
                    }
                });

                descriptors.add(descriptor);
            }
        }

        return descriptors;
    }

    private static void configureScriptDescriptor(ScriptProviderDescriptor descriptor, Function<String, InputStream> jsFileLoader) throws IOException {
        for (List<ScriptProviderMetadata> metadatas : descriptor.getProviders().values()) {
            for (ScriptProviderMetadata metadata : metadatas) {
                String fileName = metadata.getFileName();

                if (fileName == null) {
                    throw new RuntimeException("You must provide the script file name");
                }

                try (InputStream in = jsFileLoader.apply(fileName)) {
                    metadata.setCode(StreamUtil.readString(in, StandardCharsets.UTF_8));
                }

                metadata.setId(new StringBuilder("script").append("-").append(fileName).toString());

                String name = metadata.getName();

                if (name == null) {
                    name = fileName;
                }

                metadata.setName(name);
            }
        }
    }

    private boolean isScriptForSpi(Spi spi, String type) {
        if (spi instanceof ProtocolMapperSpi && (MAPPERS.equals(type) || SAML_MAPPERS.equals(type))) {
            return true;
        } else if (spi instanceof PolicySpi && POLICIES.equals(type)) {
            return true;
        } else if (spi instanceof AuthenticatorSpi && AUTHENTICATORS.equals(type)) {
            return true;
        }
        return false;
    }

    private boolean supportsDeployeableScripts(Spi spi) {
        return spi instanceof ProtocolMapperSpi || spi instanceof PolicySpi || spi instanceof AuthenticatorSpi;
    }

    private boolean isEnabled(ProviderFactory factory, Config.Scope scope) {
        if (!scope.getBoolean("enabled", true)) {
            return false;
        }
        if (factory instanceof EnvironmentDependentProviderFactory environmentDependentProviderFactory) {
            return environmentDependentProviderFactory.isSupported(scope);
        }
        return true;
    }

    private boolean isInternal(ProviderFactory<?> factory) {
        String packageName = factory.getClass().getPackage().getName();
        return packageName.startsWith("org.keycloak") && !packageName.startsWith("org.keycloak.examples");
    }

    private void checkProviders(Spi spi,
                                Map<Class<? extends Provider>, Map<String, ProviderFactory>> factoriesMap,
                                Map<Class<? extends Provider>, String> defaultProviders) {
        String defaultProvider = Config.getProvider(spi.getName());

        if (defaultProvider != null) {
            Map<String, ProviderFactory> map = factoriesMap.get(spi.getProviderClass());
            if (map == null || map.get(defaultProvider) == null) {
                throw new RuntimeException("Failed to find provider " + defaultProvider + " for " + spi.getName());
            }
        } else {
            Map<String, ProviderFactory> factories = factoriesMap.get(spi.getProviderClass());
            if (factories != null && factories.size() == 1) {
                defaultProvider = factories.values().iterator().next().getId();
            }

            if (factories != null) {
                if (defaultProvider == null) {
                    Optional<ProviderFactory> highestPriority = factories.values().stream()
                            .max(Comparator.comparing(ProviderFactory::order));
                    if (highestPriority.isPresent() && highestPriority.get().order() > 0) {
                        defaultProvider = highestPriority.get().getId();
                    }
                }
            }

            if (defaultProvider == null && (factories == null || factories.containsKey("default"))) {
                defaultProvider = "default";
            }
        }

        if (defaultProvider != null) {
            defaultProviders.put(spi.getProviderClass(), defaultProvider);
        } else {
            logger.debugv("No default provider for {0}", spi.getName());
        }
    }

    public RuntimeValue<KeycloakSessionFactoryWrapper> configureKeycloakSessionFactory(List<ParsedPersistenceXmlDescriptor> descriptors) {
        Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories = new HashMap<>();
        Map<Class<? extends Provider>, String> defaultProviders = new HashMap<>();
        Map<String, ProviderFactory> preConfiguredProviders = new HashMap<>();

        for (Map.Entry<Spi, Map<Class<? extends Provider>, Map<String, ProviderFactory>>> entry : loadFactories(preConfiguredProviders)
                .entrySet()) {
            Spi spi = entry.getKey();

            checkProviders(spi, entry.getValue(), defaultProviders);

            for (Map.Entry<Class<? extends Provider>, Map<String, ProviderFactory>> value : entry.getValue().entrySet()) {
                for (ProviderFactory factory : value.getValue().values()) {
                    factories.computeIfAbsent(spi,
                                    key -> new HashMap<>())
                            .computeIfAbsent(spi.getProviderClass(), aClass -> new HashMap<>()).put(factory.getId(), factory.getClass());
                }
            }

            if (spi instanceof JpaConnectionSpi) {
                configureUserDefinedPersistenceUnits(descriptors, factories, preConfiguredProviders, spi);
            }

            if (spi instanceof ThemeResourceSpi) {
                configureThemeResourceProviders(factories, spi);
            }
        }

        return new RuntimeValue<>(new KeycloakSessionFactoryWrapper(factories, defaultProviders, preConfiguredProviders));
    }

    private void configureThemeResourceProviders(Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories, Spi spi) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(FlatClasspathThemeResourceProviderFactory.THEME_RESOURCES);

            if (resources.hasMoreElements()) {
                // make sure theme resources are loaded using a flat classpath. if no resources are available the provider is not registered
                factories.computeIfAbsent(spi, key -> new HashMap<>()).computeIfAbsent(spi.getProviderClass(), aClass -> new HashMap<>()).put(FlatClasspathThemeResourceProviderFactory.ID, FlatClasspathThemeResourceProviderFactory.class);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to install default theme resource provider", e);
        }
    }

    private void configureUserDefinedPersistenceUnits(List<ParsedPersistenceXmlDescriptor> descriptors,
                                                      Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories,
                                                      Map<String, ProviderFactory> preConfiguredProviders, Spi spi) {
        descriptors.stream()
                .map(ParsedPersistenceXmlDescriptor::getName)
                .filter(Predicate.not("keycloak-default"::equals)).forEach((String unitName) -> {
                    NamedJpaConnectionProviderFactory factory = new NamedJpaConnectionProviderFactory();

                    factory.setUnitName(unitName);

                    factories.get(spi).get(JpaConnectionProvider.class).put(unitName, NamedJpaConnectionProviderFactory.class);
                    preConfiguredProviders.put(unitName, factory);
                });
    }

    public record KeycloakSessionFactoryWrapper(
            Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories,
            Map<Class<? extends Provider>, String> defaultProviders,
            Map<String, ProviderFactory> preConfiguredProviders) {
    }
}
