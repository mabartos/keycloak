package org.keycloak.testsuite.arquillian.containers;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;
import org.jboss.logging.Logger;
import org.keycloak.testsuite.model.MapStoreProvider;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author mhajas
 */
public class KeycloakQuarkusConfiguration implements ContainerConfiguration {

    protected static final Logger log = Logger.getLogger(KeycloakQuarkusConfiguration.class);

    private int bindHttpPortOffset = 100;
    private int bindHttpPort = 8080;
    private int bindHttpsPortOffset = 0;
    private int bindHttpsPort = Integer.parseInt(System.getProperty("auth.server.https.port", "8543"));
    private int debugPort = -1;
    private Path providersPath = Paths.get(System.getProperty("auth.server.home"));
    private int startupTimeoutInSeconds = 300;
    private String route;
    private String keycloakConfigPropertyOverrides;
    private HashMap<String, Object> keycloakConfigPropertyOverridesMap;
    private String profile;
    private String javaOpts;
    private boolean reaugmentBeforeStart;
    private String importFile = System.getProperty("migration.import.file.name");

    // Map store
    private String mapStoreProfile;

    // DB connection
    private String dbVendor;
    private String dbUrl;
    private String dbUsername;
    private String dbPassword;

    @Override
    public void validate() throws ConfigurationException {
        int basePort = getBindHttpPort();
        int newPort = basePort + bindHttpPortOffset;
        setBindHttpPort(newPort);

        int baseHttpsPort = getBindHttpsPort();
        int newHttpsPort = baseHttpsPort + bindHttpsPortOffset;
        setBindHttpsPort(newHttpsPort);

        log.info("Keycloak will listen for http on port: " + newPort + " and for https on port: " + newHttpsPort);

        if (this.keycloakConfigPropertyOverrides != null) {
            try {
                TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
                this.keycloakConfigPropertyOverridesMap = JsonSerialization.sysPropertiesAwareMapper.readValue(this.keycloakConfigPropertyOverrides, typeRef);
            } catch (IOException ex) {
                throw new ConfigurationException(ex);
            }
        }
    }

    public int getBindHttpPortOffset() {
        return bindHttpPortOffset;
    }

    public void setBindHttpPortOffset(int bindHttpPortOffset) {
        this.bindHttpPortOffset = bindHttpPortOffset;
    }

    public int getBindHttpsPortOffset() {
        return bindHttpsPortOffset;
    }

    public void setBindHttpsPortOffset(int bindHttpsPortOffset) {
        this.bindHttpsPortOffset = bindHttpsPortOffset;
    }

    public int getBindHttpsPort() {
        return this.bindHttpsPort;
    }

    public void setBindHttpsPort(int bindHttpsPort) {
        this.bindHttpsPort = bindHttpsPort;
    }

    public int getBindHttpPort() {
        return bindHttpPort;
    }

    public void setBindHttpPort(int bindHttpPort) {
        this.bindHttpPort = bindHttpPort;
    }

    public Path getProvidersPath() {
        return providersPath;
    }

    public void setProvidersPath(Path providersPath) {
        this.providersPath = providersPath;
    }

    public int getStartupTimeoutInSeconds() {
        return startupTimeoutInSeconds;
    }

    public void setStartupTimeoutInSeconds(int startupTimeoutInSeconds) {
        this.startupTimeoutInSeconds = startupTimeoutInSeconds;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getKeycloakConfigPropertyOverrides() {
        return keycloakConfigPropertyOverrides;
    }

    public void setKeycloakConfigPropertyOverrides(String keycloakConfigPropertyOverrides) {
        this.keycloakConfigPropertyOverrides = keycloakConfigPropertyOverrides;
    }

    public Map<String, Object> getKeycloakConfigPropertyOverridesMap() {
        return keycloakConfigPropertyOverridesMap;
    }

    public void setJavaOpts(String javaOpts) {
        this.javaOpts = javaOpts;
    }

    public String getJavaOpts() {
        return javaOpts;
    }

    public boolean isReaugmentBeforeStart() {
        return reaugmentBeforeStart;
    }

    public void setReaugmentBeforeStart(boolean reaugmentBeforeStart) {
        this.reaugmentBeforeStart = reaugmentBeforeStart;
    }

    public int getDebugPort() {
        return debugPort;
    }

    public void setDebugPort(int debugPort) {
        this.debugPort = debugPort;
    }

    public String getImportFile() {
        return importFile;
    }

    public void setImportFile(String importFile) {
        this.importFile = importFile;
    }

    public Optional<String> getDbVendor() {
        return Optional.ofNullable(dbVendor)
                .or(() -> Optional.ofNullable(System.getProperty("keycloak.storage.connections.vendor")))
                .filter(StringUtil::isNotBlank);
    }

    public void setDbVendor(String dbVendor) {
        this.dbVendor = dbVendor;
    }

    public Optional<String> getDbUrl() {
        return Optional.ofNullable(dbUrl)
                .or(() -> getMapStoreProvider().flatMap(MapStoreProvider::getDbUrl))
                .or(() -> Optional.ofNullable(System.getProperty("keycloak.connectionsJpa.url")))
                .filter(StringUtil::isNotBlank);
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public Optional<String> getDbUsername() {
        return Optional.ofNullable(dbUsername)
                .or(() -> getMapStoreProvider().flatMap(MapStoreProvider::getDbUsername))
                .or(() -> Optional.ofNullable(System.getProperty("keycloak.connectionsJpa.user")))
                .filter(StringUtil::isNotBlank);
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public Optional<String> getDbPassword() {
        return Optional.ofNullable(dbPassword)
                .or(() -> getMapStoreProvider().flatMap(MapStoreProvider::getDbPassword))
                .or(() -> Optional.ofNullable(System.getProperty("keycloak.connectionsJpa.password")))
                .filter(StringUtil::isNotBlank);
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public boolean isMapStore() {
        return getMapStoreProvider().isPresent();
    }

    public Optional<MapStoreProvider> getMapStoreProvider() {
        return MapStoreProvider.getProviderByAlias(mapStoreProfile)
                .or(MapStoreProvider::getCurrentProvider);
    }

    public void setMapStoreProfile(String mapStoreProfile) {
        this.mapStoreProfile = mapStoreProfile;
    }
}
