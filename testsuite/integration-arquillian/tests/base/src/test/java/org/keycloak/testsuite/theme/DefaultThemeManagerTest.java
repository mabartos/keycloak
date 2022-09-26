package org.keycloak.testsuite.theme;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.theme.Theme;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:vincent.letarouilly@gmail.com">Vincent Letarouilly</a>
 */
public class DefaultThemeManagerTest extends AbstractKeycloakTest {

    private static final String THEME_NAME = "environment-agnostic";

    @BeforeEach
    public void setUp() {
        testingClient.server().run(session -> {
            System.setProperty("existing_system_property", "Keycloak is awesome");
            session.theme().clearCache();
        });
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    // KEYCLOAK-6698
    @Test
    public void systemPropertiesSubstitutionInThemeProperties() {
        testingClient.server().run(session -> {
            try {
                Theme theme = session.theme().getTheme(THEME_NAME, Theme.Type.LOGIN);
                Assertions.assertEquals("getTheme(...) returns default theme when no matching theme found, but we need " + THEME_NAME + " theme deployed.",THEME_NAME, theme.getName());
                Assertions.assertEquals("Keycloak is awesome", theme.getProperties().getProperty("system.property.found"));
                Assertions.assertEquals("${missing_system_property}", theme.getProperties().getProperty("system.property.missing"));
                Assertions.assertEquals("defaultValue", theme.getProperties().getProperty("system.property.missing.with.default"));
            } catch (IOException e) {
                Assertions.fail(e.getMessage());
            }
        });
    }

    // KEYCLOAK-6698
    @Test
    public void environmentVariablesSubstitutionInThemeProperties() {
        testingClient.server().run(session -> {
            try {
                Theme theme = session.theme().getTheme(THEME_NAME, Theme.Type.LOGIN);
                Assertions.assertEquals("getTheme(...) returns default theme when no matching theme found, but we need " + THEME_NAME + " theme deployed.",THEME_NAME, theme.getName());
                Assertions.assertEquals("${env.MISSING_ENVIRONMENT_VARIABLE}", theme.getProperties().getProperty("env.missing"));
                Assertions.assertEquals("defaultValue", theme.getProperties().getProperty("env.missingWithDefault"));
                if (System.getenv().containsKey("HOMEPATH")) {
                    // Windows
                    Assertions.assertEquals(System.getenv().get("HOMEPATH"), theme.getProperties().getProperty("env.windowsHome"));
                } else if (System.getenv().containsKey("HOME")) {
                    // Unix
                    Assertions.assertEquals(System.getenv().get("HOME"), theme.getProperties().getProperty("env.unixHome"));
                } else {
                    Assertions.fail("No default env variable found, can't verify");
                }
            } catch (IOException e) {
                Assertions.fail(e.getMessage());
            }
        });
    }
}
