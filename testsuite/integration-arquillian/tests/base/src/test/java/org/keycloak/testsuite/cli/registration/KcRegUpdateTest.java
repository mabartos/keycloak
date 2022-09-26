package org.keycloak.testsuite.cli.registration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.client.registration.cli.config.FileConfigHandler;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.cli.KcRegExec;
import org.keycloak.testsuite.util.TempFileResource;
import org.keycloak.util.JsonSerialization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.keycloak.client.registration.cli.util.OsUtil.CMD;
import static org.keycloak.testsuite.cli.KcRegExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcRegUpdateTest extends AbstractRegCliTest {


    @Test
    public void testUpdateThoroughly() throws IOException {

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            final String realm = "test";

            loginAsUser(configFile.getFile(), serverUrl, realm, "user1", "userpass");


            // create an object so we can update it
            KcRegExec exe = execute("create --config '" + configFile.getName() + "' -o -s clientId=my_client");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);

            Assertions.assertEquals("enabled", true, client.isEnabled());
            Assertions.assertEquals("publicClient", false, client.isPublicClient());
            Assertions.assertEquals("bearerOnly", false, client.isBearerOnly());
            Assertions.assertTrue("redirectUris is empty", client.getRedirectUris().isEmpty());


            // Merge update
            exe = execute("update my_client --config '" + configFile.getName() + "' -o " +
                        " -s enabled=false -s 'redirectUris=[\"http://localhost:8980/myapp/*\"]'");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
            Assertions.assertEquals("enabled", false, client.isEnabled());
            Assertions.assertEquals("redirectUris", Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris());



            // Another merge update - test deleting an attribute, deleting a list item and adding a list item
            exe = execute("update my_client --config '" + configFile.getName() + "' -o -d redirectUris -s webOrigins+=http://localhost:8980/myapp -s webOrigins+=http://localhost:8981/myapp -d webOrigins[0]");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);

            Assertions.assertTrue("redirectUris is empty", client.getRedirectUris().isEmpty());
            Assertions.assertEquals("webOrigins", Arrays.asList("http://localhost:8981/myapp"), client.getWebOrigins());



            // Another merge update - test nested attributes and setting an attribute using json format
            // TODO KEYCLOAK-3705 Updating protocolMapper config via client registration endpoint has no effect
            /*
            exe = execute("update my_client --config '" + configFile.getName() + "' -o -s 'protocolMappers[0].config.\"id.token.claim\"=false' " +
                    "-s 'protocolMappers[4].config={\"single\": \"true\", \"attribute.nameformat\": \"Basic\", \"attribute.name\": \"Role\"}'");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
            Assertions.assertEquals("protocolMapper[0].config.\"id.token.claim\"", "false", client.getProtocolMappers().get(0).getConfig().get("id.token.claim"));
            Assertions.assertEquals("protocolMappers[4].config.single", "true", client.getProtocolMappers().get(4).getConfig().get("single"));
            Assertions.assertEquals("protocolMappers[4].config.\"attribute.nameformat\"", "Basic", client.getProtocolMappers().get(4).getConfig().get("attribute.nameformat"));
            Assertions.assertEquals("protocolMappers[4].config.\"attribute.name\"", "Role", client.getProtocolMappers().get(4).getConfig().get("attribute.name"));
            */

            // update using oidc format


            // check that using an invalid attribute key is not ignored
            exe = execute("update my_client --nonexisting --config '" + configFile.getName() + "'");

            assertExitCodeAndStreamSizes(exe, 1, 0, 2);
            Assertions.assertEquals("error message", "Unsupported option: --nonexisting", exe.stderrLines().get(0));
            Assertions.assertEquals("try help", "Try '" + CMD + " help update' for more information", exe.stderrLines().get(1));


            // try use incompatible endpoint
            exe = execute("update my_client --config '" + configFile.getName() + "' -o -s enabled=true -e oidc");

            assertExitCodeAndStreamSizes(exe, 1, 0, 1);
            Assertions.assertEquals("error message", "Failed to set attribute 'enabled' on document type 'oidc'", exe.stderrLines().get(0));



            // test overwrite from file
            exe = KcRegExec.newBuilder()
                    .argsLine("update my_client --config '" + configFile.getName() +
                            "' -o  -s clientId=my_client -s 'redirectUris=[\"http://localhost:8980/myapp/*\"]' -f -")
                    .stdin(new ByteArrayInputStream("{ \"enabled\": false }".getBytes()))
                    .execute();

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
            // web origin is not sent to the server, thus it retains the current value
            Assertions.assertEquals("webOrigins", Arrays.asList("http://localhost:8981/myapp"), client.getWebOrigins());
            Assertions.assertFalse("enabled is false", client.isEnabled());
            Assertions.assertEquals("redirectUris", Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris());



            // test using merge with file
            exe = KcRegExec.newBuilder()
                    .argsLine("update my_client --config '" + configFile.getName() +
                            "' -o -s enabled=true -m -f -")
                    .stdin(new ByteArrayInputStream("{ \"webOrigins\": [\"http://localhost:8980/myapp\"] }".getBytes()))
                    .execute();

            assertExitCodeAndStdErrSize(exe, 0, 0);

            client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
            Assertions.assertEquals("webOrigins", Arrays.asList("http://localhost:8980/myapp"), client.getWebOrigins());
            Assertions.assertTrue("enabled is true", client.isEnabled());
            Assertions.assertEquals("redirectUris", Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris());



            // remove registration access token
            exe = execute("config registration-token --config '" + configFile.getName() + "' --server " + serverUrl +
                    " --realm " + realm + " --client my_client -d");

            assertExitCodeAndStdErrSize(exe, 0, 0);

            Assertions.assertNull("my_client registration token", handler.loadConfig().ensureRealmConfigData(serverUrl, realm).getClients().get("my_client"));
        }
    }
}
