package org.keycloak.it.cli.dist;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.TestProvider;
import org.keycloak.it.tracing.CustomSamplerTestProvider;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
public class CustomTracingSamplerDistTest {

    @Test
    @TestProvider(CustomSamplerTestProvider.class)
    @Launch({"start-dev", "--tracing-enabled=true", "--tracing-sampler-type=custom-spi-sampler"})
    void testUserManagedEntityNotAddedToDefaultPU(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
    }

}
