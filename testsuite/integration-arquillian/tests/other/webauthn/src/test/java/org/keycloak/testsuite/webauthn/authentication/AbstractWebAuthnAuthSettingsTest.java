package org.keycloak.testsuite.webauthn.authentication;

import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AbstractWebAuthnAuthSettingsTest extends AbstractWebAuthnVirtualTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/webauthn/testrealm-webauthn.json"), RealmRepresentation.class);

        if (isPasswordless()) {
            makePasswordlessRequiredActionDefault(realmRepresentation);
        }

        testRealms.add(realmRepresentation);
    }

    @Override
    protected void postAfterAbstractKeycloak() {
        List<UserRepresentation> defaultUser = testRealm().users().search("username", true);
        if (defaultUser != null && !defaultUser.isEmpty()) {
            Response response = testRealm().users().delete(defaultUser.get(0).getId());
            assertThat(response, notNullValue());
            assertThat(response.getStatus(), is(204));
        }
    }

    protected static void makePasswordlessRequiredActionDefault(RealmRepresentation realm) {
        RequiredActionProviderRepresentation webAuthnProvider = realm.getRequiredActions()
                .stream()
                .filter(f -> f.getProviderId().equals(WebAuthnRegisterFactory.PROVIDER_ID))
                .findFirst()
                .orElse(null);
        assertThat(webAuthnProvider, notNullValue());

        webAuthnProvider.setEnabled(false);

        RequiredActionProviderRepresentation webAuthnPasswordlessProvider = realm.getRequiredActions()
                .stream()
                .filter(f -> f.getProviderId().equals(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID))
                .findFirst()
                .orElse(null);
        assertThat(webAuthnPasswordlessProvider, notNullValue());

        webAuthnPasswordlessProvider.setEnabled(true);
        webAuthnPasswordlessProvider.setDefaultAction(true);
    }

    /*
    checkAvailableAuths
    checkAllowCredentials
    userVerification
    rpId
    timeout
    userHandle
    userIdentified
    credentialType
    error Different USER
    error user not found
     */
}
