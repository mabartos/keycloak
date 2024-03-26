/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.it.cli.dist;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.DistributionType;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;

import java.net.ConnectException;
import java.net.SocketException;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DistributionTest(keepAlive = true,
        defaultOptions = {"--health-enabled=true", "--metrics-enabled=true"},
        requestPort = 9000,
        containerExposedPorts = {9000, 8080, 9005})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ManagementDistTest {

    @Test
    @Order(1)
    @Launch({"start", "--management-enabled=true", "--hostname=hostname", "--http-enabled=false"})
    void testManagementNoHttps(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Management interface listening on");
        cliResult.assertMessage("Key material not provided to setup HTTPS. Please configure your keys/certificates or start the server in development mode.");
    }

    @Test
    @Launch({"start-dev", "--management-enabled=false"})
    void testManagementDisabled(LaunchResult result, KeycloakDistribution distribution) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoMessage("Management interface listening on");

        assertThrows(SocketException.class, () -> when().get("/"), "Connection refused must be thrown");
        assertThrows(SocketException.class, () -> when().get("/health"), "Connection refused must be thrown");

        distribution.setRequestPort(8080);

        when().get("/health").then()
                .statusCode(200);
    }

    @Test
    @Launch({"start-dev", "--management-enabled=true"})
    void testManagementEnabled(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Management interface listening on http://0.0.0.0:9000");

        when().get("/").then()
                .statusCode(200)
                .and()
                .body(is("Keycloak Management Interface"));
        when().get("/health").then()
                .statusCode(200);
        when().get("/health/live").then()
                .statusCode(200);
        when().get("/health/ready").then()
                .statusCode(200);
        when().get("/metrics").then()
                .statusCode(200);
    }

    @Test
    @Launch({"start-dev", "--management-enabled=true", "--management-port=9005"})
    void testManagementDifferentPort(LaunchResult result, KeycloakDistribution distribution) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Management interface listening on http://0.0.0.0:9005");

        distribution.setRequestPort(9005);

        when().get("/").then()
                .statusCode(200)
                .and()
                .body(is("Keycloak Management Interface"));
        when().get("/health").then()
                .statusCode(200);
        when().get("/health/live").then()
                .statusCode(200);
        when().get("/health/ready").then()
                .statusCode(200);
        when().get("/metrics").then()
                .statusCode(200);
    }

    @Test
    @Launch({"start-dev", "--management-enabled=true", "--management-relative-path=/management"})
    void testManagementDifferentRelativePath(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Management interface listening on http://0.0.0.0:9000");

        when().get("/management").then()
                .statusCode(200)
                .and()
                .body(is("Keycloak Management Interface"));
        when().get("/management/health").then()
                .statusCode(200);
        when().get("/health").then()
                .statusCode(404);
        when().get("/management/health/live").then()
                .statusCode(200);
        when().get("/management/health/ready").then()
                .statusCode(200);
        when().get("/management/metrics").then()
                .statusCode(200);
        when().get("/metrics").then()
                .statusCode(404);
    }

    @Test
    @Launch({"start-dev", "--management-enabled=true", "--management-host=localhost"})
    void testManagementDifferentHost(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Management interface listening on http://localhost:9000");

        // If running in container, we cannot access the localhost due to network host settings
        if (DistributionType.isContainerDist()) return;

        when().get("/").then()
                .statusCode(200)
                .and()
                .body(is("Keycloak Management Interface"));
        when().get("/health").then()
                .statusCode(200);
        when().get("/health/live").then()
                .statusCode(200);
        when().get("/health/ready").then()
                .statusCode(200);
        when().get("/metrics").then()
                .statusCode(200);
    }
}
