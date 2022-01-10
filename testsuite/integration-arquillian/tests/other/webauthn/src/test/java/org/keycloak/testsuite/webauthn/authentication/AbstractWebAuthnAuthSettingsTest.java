package org.keycloak.testsuite.webauthn.authentication;

import org.junit.Test;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.utils.WebAuthnRealmData;

import java.io.Closeable;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.util.WaitUtils.pause;

public class AbstractWebAuthnAuthSettingsTest extends AbstractWebAuthnVirtualTest {

    @Test
    public void checkAllowCredentials() {
        getVirtualAuthManager().useAuthenticator(getDefaultAuthenticatorOptions().setHasResidentKey(true));

        getVirtualAuthManager().getCurrent().getAuthenticator().removeAllCredentials();
        getVirtualAuthManager().getCurrent().getAuthenticator().addCredential(getDefaultResidentKeyCredential());

        registerDefaultUser();
        logout();
        authenticateDefaultUser();
    }

    @Test
    public void timeout() throws IOException {
        final Integer TIMEOUT = 3; //seconds

        registerDefaultUser();
        logout();

        getVirtualAuthManager().removeAuthenticator();

        try (Closeable u = getWebAuthnRealmUpdater().setWebAuthnPolicyCreateTimeout(TIMEOUT).update()) {
            WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            assertThat(realmData.getCreateTimeout(), is(TIMEOUT));

            authenticateDefaultUser(false);
            pause((TIMEOUT + 2) * 1000);
            webAuthnErrorPage.assertCurrent();
            assertThat(webAuthnErrorPage.getError(), containsString("Failed to authenticate by the Security key."));
        }
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
