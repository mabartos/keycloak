package org.keycloak.testsuite.arquillian.containers;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.keycloak.testsuite.arquillian.SuiteContext;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author mhajas
 */
public class KeycloakQuarkusServerDeployableContainer implements DeployableContainer<KeycloakQuarkusConfiguration> {

    private static final Logger log = Logger.getLogger(KeycloakQuarkusServerDeployableContainer.class);

    private KeycloakQuarkusConfiguration configuration;
    private Process container;
    private static AtomicBoolean restart = new AtomicBoolean();

    @Inject
    private Instance<SuiteContext> suiteContext;

    private List<String> additionalBuildArgs = Collections.emptyList();

    @Override
    public Class<KeycloakQuarkusConfiguration> getConfigurationClass() {
        return KeycloakQuarkusConfiguration.class;
    }

    @Override
    public void setup(KeycloakQuarkusConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() throws LifecycleException {
        try {
            importRealm();
            container = startContainer();
            waitForReadiness();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() throws LifecycleException {
        container.destroy();
        try {
            container.waitFor(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            container.destroyForcibly();
        }
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return null;
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        log.infof("Trying to deploy: " + archive.getName());

        try {
            deployArchiveToServer(archive);
            restartServer();
        } catch (Exception e) {
            throw new DeploymentException(e.getMessage(),e);
        }

        return new ProtocolMetaData();
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        File wrkDir = configuration.getProvidersPath().resolve("providers").toFile();
        try {
            Files.deleteIfExists(wrkDir.toPath().resolve(archive.getName()));
            restartServer();
        } catch (Exception e) {
            throw new DeploymentException(e.getMessage(),e);
        }
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {

    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {

    }

    private void importRealm() throws IOException, InterruptedException, URISyntaxException {
        if (configuration.getImportFile() != null) {
            final List<String> commands = new ArrayList<>();
            commands.add(getCommand());
            commands.add("import");
            System.out.println("PROPS");
            Path url = Paths.get(getClass().getResource("/migration-test/" + System.getProperty("migration.import.file.name")).toURI());
            System.out.println(url);
            System.out.println(System.getProperty("auth.migration.server.home"));
            System.out.println(System.getProperty("migration.import.file.name"));
            System.out.println(System.getProperty("migration.import.file"));
            System.out.println(System.getProperty("keycloak.migration.file"));
            System.out.println(configuration.getImportFile());
            File wrkDir = configuration.getProvidersPath().resolve("bin").toFile();
            Path newUrl = wrkDir.toPath().relativize(url);
            System.out.println(newUrl);
            //System.out.println(path);

            //commands.add("--file=" + "../../../test-classes/migration-test/migration-realm-17.0.0.json");
            commands.add("--file=" + newUrl);
            final ProcessBuilder builder = getBaseProcessBuilder(commands.toArray(new String[0]));
            builder.start();
        } else {
            System.out.println("NOT HERE");
        }
    }

    private Process startContainer() throws IOException {
        final ProcessBuilder builder = getBaseProcessBuilder(getProcessCommands());

        String javaOpts = configuration.getJavaOpts();

        if (javaOpts != null) {
            builder.environment().put("JAVA_OPTS", javaOpts);
        }

        builder.environment().put("KEYCLOAK_ADMIN", "admin");
        builder.environment().put("KEYCLOAK_ADMIN_PASSWORD", "admin");

        if (restart.compareAndSet(false, true)) {
            FileUtils.deleteDirectory(configuration.getProvidersPath().resolve("data").toFile());
        }

        return builder.start();
    }

    private ProcessBuilder getBaseProcessBuilder(String... commands) {
        final ProcessBuilder pb = new ProcessBuilder(commands);
        File wrkDir = configuration.getProvidersPath().resolve("bin").toFile();
        return pb.directory(wrkDir).inheritIO();
    }

    private String[] getProcessCommands() {
        List<String> commands = new ArrayList<>();
        commands.add(getCommand());
        commands.add("-v");
        commands.add("start");
        commands.add("--http-enabled=true");

        if (Boolean.parseBoolean(System.getProperty("auth.server.debug", "false"))) {
            commands.add("--debug");
            if (configuration.getDebugPort() > 0) {
                commands.add(Integer.toString(configuration.getDebugPort()));
            } else {
                commands.add(System.getProperty("auth.server.debug.port", "5005"));
            }
        }

        commands.add("--http-port=" + configuration.getBindHttpPort());
        commands.add("--https-port=" + configuration.getBindHttpsPort());

        if (configuration.getRoute() != null) {
            commands.add("-Djboss.node.name=" + configuration.getRoute());
        }

        // only run auto-build during restarts or when running cluster tests
        if (restart.get() || "ha".equals(System.getProperty("auth.server.quarkus.cluster.config"))) {
            commands.add("--auto-build");
            commands.add("--http-relative-path=/auth");

            String cacheMode = System.getProperty("auth.server.quarkus.cluster.config", "local");

            if ("local".equals(cacheMode)) {
                commands.add("--cache=local");
            } else {
                commands.add("--cache-config-file=cluster-" + cacheMode + ".xml");
            }
        }

        commands.addAll(getAdditionalBuildArgs());

        return commands.toArray(new String[0]);
    }

    private void waitForReadiness() throws MalformedURLException, LifecycleException {
        SuiteContext suiteContext = this.suiteContext.get();
        //TODO: not sure if the best endpoint but it makes sure that everything is properly initialized. Once we have
        // support for MP Health this should change
        URL contextRoot = new URL(getBaseUrl(suiteContext) + "/auth/realms/master/");
        HttpURLConnection connection;
        long startTime = System.currentTimeMillis();

        while (true) {
            if (System.currentTimeMillis() - startTime > getStartTimeout()) {
                stop();
                throw new IllegalStateException("Timeout [" + getStartTimeout() + "] while waiting for Quarkus server");
            }

            try {
                // wait before checking for opening a new connection
                Thread.sleep(1000);
                if ("https".equals(contextRoot.getProtocol())) {
                    HttpsURLConnection httpsConnection = (HttpsURLConnection) (connection = (HttpURLConnection) contextRoot.openConnection());
                    httpsConnection.setSSLSocketFactory(createInsecureSslSocketFactory());
                    httpsConnection.setHostnameVerifier(createInsecureHostnameVerifier());
                } else {
                    connection = (HttpURLConnection) contextRoot.openConnection();
                }

                connection.setReadTimeout((int) getStartTimeout());
                connection.setConnectTimeout((int) getStartTimeout());
                connection.connect();

                if (connection.getResponseCode() == 200) {
                    break;
                }

                connection.disconnect();
            } catch (Exception ignore) {
            }
        }
        
        log.infof("Keycloak is ready at %s", contextRoot);
    }

    private URL getBaseUrl(SuiteContext suiteContext) throws MalformedURLException {
        URL baseUrl = suiteContext.getAuthServerInfo().getContextRoot();

        // might be running behind a load balancer
        if ("https".equals(baseUrl.getProtocol())) {
            baseUrl = new URL(baseUrl.toString().replace(String.valueOf(baseUrl.getPort()), String.valueOf(configuration.getBindHttpsPort())));
        } else {
            baseUrl = new URL(baseUrl.toString().replace(String.valueOf(baseUrl.getPort()), String.valueOf(configuration.getBindHttpPort())));
        }
        return baseUrl;
    }

    private HostnameVerifier createInsecureHostnameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        };
    }

    private SSLSocketFactory createInsecureSslSocketFactory() throws IOException {
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
            }

            public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }};

        SSLContext sslContext;
        SSLSocketFactory socketFactory;

        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            socketFactory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IOException("Can't create unsecure trust manager");
        }
        return socketFactory;
    }

    private long getStartTimeout() {
        return TimeUnit.SECONDS.toMillis(configuration.getStartupTimeoutInSeconds());
    }

    public void resetConfiguration() {
        additionalBuildArgs = Collections.emptyList();
    }

    private void deployArchiveToServer(Archive<?> archive) throws IOException {
        File providersDir = configuration.getProvidersPath().resolve("providers").toFile();
        InputStream zipStream = archive.as(ZipExporter.class).exportAsInputStream();
        Files.copy(zipStream, providersDir.toPath().resolve(archive.getName()), StandardCopyOption.REPLACE_EXISTING);
    }

    public void restartServer() throws Exception {
        stop();
        start();
    }

    private static String getCommand() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return "kc.bat";
        }
        return "./kc.sh";
    }

    public List<String> getAdditionalBuildArgs() {
        return additionalBuildArgs;
    }

    public void setAdditionalBuildArgs(List<String> newArgs) {
        additionalBuildArgs = newArgs;
    }
}
