package org.keycloak.testsuite.cli.registration;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.client.registration.cli.config.ConfigData;
import org.keycloak.client.registration.cli.config.FileConfigHandler;
import org.keycloak.common.Profile;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.cli.KcRegExec;
import org.keycloak.testsuite.util.TempFileResource;
import org.keycloak.util.JsonSerialization;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.cli.KcRegExec.execute;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcRegCreateTest extends AbstractRegCliTest {

    @BeforeEach
    public void assumeTLSEnabled() {
        Assumptions.assumeTrue(AUTH_SERVER_SSL_REQUIRED);
    }

    @Test
    public void testCreateWithRealmOverride() throws IOException {

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            // authenticate as a regular user against one realm
            KcRegExec exe = execute("config credentials -x --config '" + configFile.getName() +
                    "' --insecure --server " + oauth.AUTH_SERVER_ROOT + " --realm master --user admin --password admin");

            assertExitCodeAndStreamSizes(exe, 0, 0, 3);

            // use initial token of another realm with server, and realm override
            String token = issueInitialAccessToken("test");
            exe = execute("create --config '" + configFile.getName() + "' --insecure --server " + oauth.AUTH_SERVER_ROOT + " --realm test -s clientId=my_first_client -t " + token);

            assertExitCodeAndStreamSizes(exe, 0, 0, 3);
        }
    }


    @Test
    public void testCreateThoroughly() throws IOException {

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {
            // set initial access token in config
            String token = issueInitialAccessToken("test");

            final String realm = "test";

            KcRegExec exe = execute("config initial-token -x --config '" + configFile.getName() +
                    "' --insecure --server " + oauth.AUTH_SERVER_ROOT + " --realm " + realm + " " + token);

            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            // check that current server, realm, and initial token are saved in the file
            ConfigData config = handler.loadConfig();
            Assertions.assertEquals("Config serverUrl", oauth.AUTH_SERVER_ROOT, config.getServerUrl());
            Assertions.assertEquals("Config realm", realm, config.getRealm());
            Assertions.assertEquals("Config initial access token", token, config.ensureRealmConfigData(oauth.AUTH_SERVER_ROOT, realm).getInitialToken());

            // create configuration from file using stdin redirect ... output an object
            String content = "{\n" +
                    "        \"clientId\": \"my_client\",\n" +
                    "        \"enabled\": true,\n" +
                    "        \"redirectUris\": [\"http://localhost:8980/myapp/*\"],\n" +
                    "        \"serviceAccountsEnabled\": true,\n" +
                    "        \"name\": \"My Client App\",\n" +
                    "        \"implicitFlowEnabled\": false,\n" +
                    "        \"publicClient\": true,\n" +
                    "        \"protocol\": \"openid-connect\",\n" +
                    "        \"webOrigins\": [\"http://localhost:8980/myapp\"],\n" +
                    "        \"consentRequired\": false,\n" +
                    "        \"baseUrl\": \"http://localhost:8980/myapp\",\n" +
                    "        \"rootUrl\": \"http://localhost:8980/myapp\",\n" +
                    "        \"bearerOnly\": true,\n" +
                    "        \"standardFlowEnabled\": true\n" +
                    "}";

            try (TempFileResource tmpFile = new TempFileResource(initTempFile(".json", content))) {

                exe = execute("create --insecure --config '" + configFile.getName() + "' -o -f - < '" + tmpFile.getName() + "'");

                assertExitCodeAndStdErrSize(exe, 0, 2);

                ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
                Assertions.assertNotNull("id", client.getId());
                Assertions.assertEquals("clientId", "my_client", client.getClientId());
                Assertions.assertEquals("enabled", true, client.isEnabled());
                Assertions.assertEquals("redirectUris", Arrays.asList("http://localhost:8980/myapp/*"), client.getRedirectUris());
                Assertions.assertEquals("serviceAccountsEnabled", true, client.isServiceAccountsEnabled());
                Assertions.assertEquals("name", "My Client App", client.getName());
                Assertions.assertEquals("implicitFlowEnabled", false, client.isImplicitFlowEnabled());
                Assertions.assertEquals("publicClient", true, client.isPublicClient());
                // note there is no server-side check if protocol is supported
                Assertions.assertEquals("protocol", "openid-connect", client.getProtocol());
                Assertions.assertEquals("webOrigins", Arrays.asList("http://localhost:8980/myapp"), client.getWebOrigins());
                Assertions.assertEquals("consentRequired", false, client.isConsentRequired());
                Assertions.assertEquals("baseUrl", "http://localhost:8980/myapp", client.getBaseUrl());
                Assertions.assertEquals("rootUrl", "http://localhost:8980/myapp", client.getRootUrl());
                Assertions.assertEquals("bearerOnly", true, client.isStandardFlowEnabled());
                Assertions.assertNull("mappers are null", client.getProtocolMappers());

                // create configuration from file as a template and override clientId and other attributes ... output an object
                exe = execute("create --insecure --config '" + configFile.getName() + "' -o -f '" + tmpFile.getName() +
                        "' -s clientId=my_client2 -s enabled=false -s 'redirectUris=[\"http://localhost:8980/myapp2/*\"]'" +
                        " -s 'name=My Client App II' -s protocol=openid-connect -s 'webOrigins=[\"http://localhost:8980/myapp2\"]'" +
                        " -s baseUrl=http://localhost:8980/myapp2 -s rootUrl=http://localhost:8980/myapp2");

                assertExitCodeAndStdErrSize(exe, 0, 2);

                ClientRepresentation client2 = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
                Assertions.assertNotNull("id", client2.getId());
                Assertions.assertEquals("clientId", "my_client2", client2.getClientId());
                Assertions.assertEquals("enabled", false, client2.isEnabled());
                Assertions.assertEquals("redirectUris", Arrays.asList("http://localhost:8980/myapp2/*"), client2.getRedirectUris());
                Assertions.assertEquals("serviceAccountsEnabled", true, client2.isServiceAccountsEnabled());
                Assertions.assertEquals("name", "My Client App II", client2.getName());
                Assertions.assertEquals("implicitFlowEnabled", false, client2.isImplicitFlowEnabled());
                Assertions.assertEquals("publicClient", true, client2.isPublicClient());
                Assertions.assertEquals("protocol", "openid-connect", client2.getProtocol());
                Assertions.assertEquals("webOrigins", Arrays.asList("http://localhost:8980/myapp2"), client2.getWebOrigins());
                Assertions.assertEquals("consentRequired", false, client2.isConsentRequired());
                Assertions.assertEquals("baseUrl", "http://localhost:8980/myapp2", client2.getBaseUrl());
                Assertions.assertEquals("rootUrl", "http://localhost:8980/myapp2", client2.getRootUrl());
                Assertions.assertEquals("bearerOnly", true, client2.isStandardFlowEnabled());
                Assertions.assertNull("mappers are null", client2.getProtocolMappers());


                // check that using an invalid attribute key is not ignored
                exe = execute("create --config '" + configFile.getName() + "' -o -f '" + tmpFile.getName() + "' -s client_id=my_client3");

                assertExitCodeAndStreamSizes(exe, 1, 0, 1);
                Assertions.assertEquals("Failed to set attribute 'client_id' on document type 'default'", exe.stderrLines().get(0));
            }

            // simple create, output an id
            exe = execute("create --insecure --config '" + configFile.getName() + "' -i -s clientId=my_client3");

            assertExitCodeAndStreamSizes(exe, 0, 1, 2);
            Assertions.assertEquals("only clientId returned", "my_client3", exe.stdoutLines().get(0));

            // simple create, default output
            exe = execute("create --insecure --config '" + configFile.getName() + "' -s clientId=my_client4");

            assertExitCodeAndStreamSizes(exe, 0, 0, 3);
            Assertions.assertEquals("only clientId returned", "Registered new client with client_id 'my_client4'", exe.stderrLines().get(2));



            // create using oidc endpoint - autodetect format
            content = "        {\n" +
                    "            \"redirect_uris\" : [ \"http://localhost:8980/myapp/*\" ],\n" +
                    "            \"grant_types\" : [ \"authorization_code\", \"client_credentials\", \"refresh_token\" ],\n" +
                    "            \"response_types\" : [ \"code\", \"none\" ],\n" +
                    "            \"client_name\" : \"My Client App\",\n" +
                    "            \"client_uri\" : \"http://localhost:8980/myapp\"\n" +
                    "        }";

            try (TempFileResource tmpFile = new TempFileResource(initTempFile(".json", content))) {

                exe = execute("create --insecure --config '" + configFile.getName() + "' -s 'client_name=My Client App V' " +
                        " -s 'redirect_uris=[\"http://localhost:8980/myapp5/*\"]' -s client_uri=http://localhost:8980/myapp5" +
                        " -o -f - < '" + tmpFile.getName() + "'");

                assertExitCodeAndStdErrSize(exe, 0, 2);

                OIDCClientRepresentation client = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);

                Assertions.assertNotNull("clientId", client.getClientId());
                Assertions.assertEquals("redirect_uris", Arrays.asList("http://localhost:8980/myapp5/*"), client.getRedirectUris());
                Assertions.assertEquals("grant_types", Arrays.asList("authorization_code", "client_credentials", "refresh_token"), client.getGrantTypes());
                Assertions.assertEquals("response_types", Arrays.asList("code", "none"), client.getResponseTypes());
                Assertions.assertEquals("client_name", "My Client App V", client.getClientName());
                Assertions.assertEquals("client_uri", "http://localhost:8980/myapp5", client.getClientUri());



                // try use incompatible endpoint override
                exe = execute("create --config '" + configFile.getName() + "' -e default -f '" + tmpFile.getName() + "'");

                assertExitCodeAndStreamSizes(exe, 1, 0, 1);
                Assertions.assertEquals("Error message", "Attribute 'redirect_uris' not supported on document type 'default'", exe.stderrLines().get(0));
            }


            // test create saml formated xml - format autodetection
            File samlSpMetaFile = new File(System.getProperty("user.dir") + "/src/test/resources/cli/kcreg/saml-sp-metadata.xml");
            Assertions.assertTrue("saml-sp-metadata.xml exists", samlSpMetaFile.isFile());

            exe = execute("create --insecure --config '" + configFile.getName() + "' -o -f - < '" + samlSpMetaFile.getAbsolutePath() + "'");

            assertExitCodeAndStdErrSize(exe, 0, 2);

            ClientRepresentation client = JsonSerialization.readValue(exe.stdout(), ClientRepresentation.class);
            Assertions.assertNotNull("id", client.getId());
            Assertions.assertEquals("clientId", "http://localhost:8080/sales-post-enc/", client.getClientId());
            Assertions.assertEquals("redirectUris", Arrays.asList("http://localhost:8081/sales-post-enc/saml"), client.getRedirectUris());
            Assertions.assertEquals("attributes.saml_name_id_format", "username", client.getAttributes().get("saml_name_id_format"));
            Assertions.assertEquals("attributes.saml_assertion_consumer_url_post", "http://localhost:8081/sales-post-enc/saml", client.getAttributes().get("saml_assertion_consumer_url_post"));
            Assertions.assertEquals("attributes.saml.signature.algorithm", "RSA_SHA256", client.getAttributes().get("saml.signature.algorithm"));


            // delete initial token
            exe = execute("config initial-token --config '" + configFile.getName() + "' --insecure --server " + serverUrl + " --realm " + realm + " --delete");
            assertExitCodeAndStreamSizes(exe, 0, 0, 0);

            config = handler.loadConfig();
            Assertions.assertNull("initial token == null", config.ensureRealmConfigData(serverUrl, realm).getInitialToken());
        }
    }

    @Test
    public void testCreateWithAuthorizationServices() throws IOException {
        ProfileAssume.assumeFeatureEnabled(Profile.Feature.AUTHORIZATION);

        FileConfigHandler handler = initCustomConfigFile();

        try (TempFileResource configFile = new TempFileResource(handler.getConfigFile())) {

            KcRegExec exe = execute("config credentials -x --config '" + configFile.getName() +
                    "' --insecure --server " + oauth.AUTH_SERVER_ROOT + " --realm master --user admin --password admin");
            assertExitCodeAndStreamSizes(exe, 0, 0, 3);

            String token = issueInitialAccessToken("test");
            exe = execute("create --config '" + configFile.getName() + "' --insecure --server " + oauth.AUTH_SERVER_ROOT + " --realm test -s clientId=authz-client -s authorizationServicesEnabled=true -t " + token);
            assertExitCodeAndStreamSizes(exe, 0, 0, 3);

            RealmResource realm = adminClient.realm("test");
            ClientsResource clients = realm.clients();
            ClientRepresentation clientRep = clients.findByClientId("authz-client").get(0);

            ClientResource client = clients.get(clientRep.getId());

            clientRep = client.toRepresentation();
            Assertions.assertTrue(clientRep.getAuthorizationServicesEnabled());

            ResourceServerRepresentation settings = client.authorization().getSettings();

            Assertions.assertEquals(PolicyEnforcementMode.ENFORCING, settings.getPolicyEnforcementMode());
            Assertions.assertTrue(settings.isAllowRemoteResourceManagement());

            List<RoleRepresentation> roles = client.roles().list();

            Assertions.assertEquals(1, roles.size());
            Assertions.assertEquals("uma_protection", roles.get(0).getName());

            // create using oidc endpoint - autodetect format
            String content = "        {\n" +
                    "            \"redirect_uris\" : [ \"http://localhost:8980/myapp/*\" ],\n" +
                    "            \"grant_types\" : [ \"authorization_code\", \"client_credentials\", \"refresh_token\", \"" + OAuth2Constants.UMA_GRANT_TYPE + "\" ],\n" +
                    "            \"response_types\" : [ \"code\", \"none\" ],\n" +
                    "            \"client_name\" : \"My Reg Authz\",\n" +
                    "            \"client_uri\" : \"http://localhost:8980/myapp\"\n" +
                    "        }";

            try (TempFileResource tmpFile = new TempFileResource(initTempFile(".json", content))) {

                exe = execute("create --insecure --config '" + configFile.getName() + "' -s 'client_name=My Reg Authz' --realm test -t " + token +
                        " -s 'redirect_uris=[\"http://localhost:8980/myapp5/*\"]' -s client_uri=http://localhost:8980/myapp5" +
                        " -o -f - < '" + tmpFile.getName() + "'");

                assertExitCodeAndStdErrSize(exe, 0, 2);

                OIDCClientRepresentation oidcClient = JsonSerialization.readValue(exe.stdout(), OIDCClientRepresentation.class);

                Assertions.assertNotNull("clientId", oidcClient.getClientId());
                Assertions.assertEquals("redirect_uris", Arrays.asList("http://localhost:8980/myapp5/*"), oidcClient.getRedirectUris());
                Assertions.assertThat("grant_types", oidcClient.getGrantTypes(), Matchers.containsInAnyOrder("authorization_code", "client_credentials", "refresh_token", OAuth2Constants.UMA_GRANT_TYPE));
                Assertions.assertEquals("response_types", Arrays.asList("code", "none"), oidcClient.getResponseTypes());
                Assertions.assertEquals("client_name", "My Reg Authz", oidcClient.getClientName());
                Assertions.assertEquals("client_uri", "http://localhost:8980/myapp5", oidcClient.getClientUri());

                client = clients.get(oidcClient.getClientId());

                clientRep = client.toRepresentation();
                Assertions.assertTrue(clientRep.getAuthorizationServicesEnabled());

                settings = client.authorization().getSettings();

                Assertions.assertEquals(PolicyEnforcementMode.ENFORCING, settings.getPolicyEnforcementMode());
                Assertions.assertTrue(settings.isAllowRemoteResourceManagement());

                roles = client.roles().list();

                Assertions.assertEquals(1, roles.size());
                Assertions.assertEquals("uma_protection", roles.get(0).getName());

                UserRepresentation serviceAccount = realm.users().search(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + clientRep.getClientId()).get(0);
                Assertions.assertNotNull(serviceAccount);
                List<RoleRepresentation> serviceAccountRoles = realm.users().get(serviceAccount.getId()).roles().clientLevel(clientRep.getId()).listAll();
                Assertions.assertTrue(serviceAccountRoles.stream().anyMatch(roleRepresentation -> "uma_protection".equals(roleRepresentation.getName())));
            }
        }
    }
}
