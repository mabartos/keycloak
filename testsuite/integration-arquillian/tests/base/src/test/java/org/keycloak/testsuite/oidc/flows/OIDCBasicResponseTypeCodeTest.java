/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.oidc.flows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.events.Details;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.OAuthClient;

import java.util.Collections;
import java.util.List;

/**
 * Test for response_type=code
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCBasicResponseTypeCodeTest extends AbstractOIDCResponseTypeTest {

    @BeforeEach
    public void clientConfiguration() {
        clientManagerBuilder().standardFlow(true).implicitFlow(false);

        oauth.clientId("test-app");
        oauth.responseType(OIDCResponseType.CODE);
    }


    @Override
    protected boolean isFragment() {
        return false;
    }

    @Override
    protected List<IDToken> testAuthzResponseAndRetrieveIDTokens(OAuthClient.AuthorizationEndpointResponse authzResponse, EventRepresentation loginEvent) {
        Assertions.assertEquals(OIDCResponseType.CODE, loginEvent.getDetails().get(Details.RESPONSE_TYPE));

        Assertions.assertNull(authzResponse.getAccessToken());
        Assertions.assertNull(authzResponse.getIdToken());

        OAuthClient.AccessTokenResponse authzResponse2 = sendTokenRequestAndGetResponse(loginEvent);
        IDToken idToken2 = oauth.verifyIDToken(authzResponse2.getIdToken());

        // Validate "at_hash"
        assertValidAccessTokenHash(idToken2.getAccessTokenHash(), authzResponse2.getAccessToken());

        // Validate if token_type is null
        Assertions.assertNull(authzResponse.getTokenType());

        // Validate if expires_in is null
        Assertions.assertNull(authzResponse.getExpiresIn());

        return Collections.singletonList(idToken2);
    }


    @Test
    public void nonceNotUsed() {
        EventRepresentation loginEvent = loginUser(null);

        OAuthClient.AuthorizationEndpointResponse authzResponse = new OAuthClient.AuthorizationEndpointResponse(oauth, false);
        List<IDToken> idTokens = testAuthzResponseAndRetrieveIDTokens(authzResponse, loginEvent);
        for (IDToken idToken : idTokens) {
            Assertions.assertNull(idToken.getNonce());
        }
    }

    @Test
    public void errorStandardFlowNotAllowed() throws Exception {
        super.validateErrorStandardFlowNotAllowed();
    }
}
