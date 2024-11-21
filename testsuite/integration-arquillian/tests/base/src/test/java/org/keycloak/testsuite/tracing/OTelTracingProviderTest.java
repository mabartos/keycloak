package org.keycloak.testsuite.tracing;

import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.containers.AbstractQuarkusDeployableContainer;
import org.keycloak.tracing.TracingProvider;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;


@EnableFeature(Profile.Feature.OPENTELEMETRY)
@AuthServerContainerExclude(value = AuthServerContainerExclude.AuthServer.UNDERTOW)
public class OTelTracingProviderTest extends AbstractTestRealmKeycloakTest {

    @ArquillianResource
    protected ContainerController controller;

    private static boolean initialized = false;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Before
    public void setContainer() {
        if (!initialized) {
            startContainer();
            //initialized = true;
        }
    }

    void startContainer() {
        assertThat(suiteContext.getAuthServerInfo().isQuarkus(), CoreMatchers.is(true));
        var containerQualifier = suiteContext.getAuthServerInfo().getQualifier();
        AbstractQuarkusDeployableContainer container = (AbstractQuarkusDeployableContainer) suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();
        try {
            controller.stop(containerQualifier);
            container.setAdditionalBuildArgs(List.of("--tracing-enabled=true"));
            controller.start(containerQualifier);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test() {
        testingClient.server().run(session -> {
            var provider = session.getProvider(TracingProvider.class);
            System.err.println(provider);
            System.err.println(provider.getCurrentSpan());
        });
    }
}
