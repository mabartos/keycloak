package org.keycloak.quarkus.runtime.configuration.test;

import io.smallrye.config.SmallRyeConfig;
import org.hibernate.dialect.MariaDBDialect;
import org.junit.Test;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DatasourcesConfigurationTest extends AbstractConfigurationTest {

    @Test
    public void testPropertyMapping() {
        ConfigArgsConfigSource.setCliArgs("--db-user-store-vendor=mariadb", "--db-user-store-url=jdbc:mariadb://localhost/keycloak");
        SmallRyeConfig config = createConfig();
        assertEquals(MariaDBDialect.class.getName(), config.getConfigValue("kc.db-user-store-dialect").getValue());
        assertEquals("jdbc:mariadb://localhost/keycloak", config.getConfigValue("quarkus.datasource.\"user-store\".jdbc.url").getValue());
    }

    @Test
    public void testDatabaseDriverSetExplicitly() {
        ConfigArgsConfigSource.setCliArgs("--db-user-store-vendor=mssql", "--db-user-store-url=jdbc:sqlserver://localhost/keycloak");
        System.setProperty("kc.db-user-store-driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        System.setProperty("kc.transaction-xa-enabled", "false");
        assertTrue(ConfigArgsConfigSource.getAllCliArgs().contains("--db-user-store-vendor=mssql"));
        SmallRyeConfig config = createConfig();
        //assertEquals("jdbc:sqlserver://localhost/keycloak", config.getConfigValue("quarkus.datasource.\"user-store\".jdbc.url").getValue());
        assertEquals("mssql", config.getConfigValue("kc.db-user-store-vendor").getValue());
        assertEquals("mssql", config.getConfigValue("quarkus.datasource.\"user-store\".db-kind").getValue());
        assertEquals("com.microsoft.sqlserver.jdbc.SQLServerDriver", config.getConfigValue("quarkus.datasource.\"user-store\".jdbc.driver").getValue());
        assertEquals("enabled", config.getConfigValue("quarkus.datasource.\"user-store\".jdbc.transactions").getValue());
    }
}
