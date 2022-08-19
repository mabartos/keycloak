/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.policy;

import org.junit.Test;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class OtpPolicyTest extends AbstractTestRealmKeycloakTest {

    @Test
    public void defaultValues() {
        RealmRepresentation realm = testRealm().toRepresentation();

        OTPPolicy policy = OTPPolicy.DEFAULT_POLICY;
        assertThat(policy, notNullValue());

        assertOtpPolicy(realm,
                policy.getAlgorithm(),
                policy.getDigits(),
                policy.getInitialCounter(),
                policy.getPeriod(),
                policy.getType(),
                policy.getLookAheadWindow(),
                policy.isCodeReusable());

        assertThat(realm.isOtpPolicyCodeReusable(), is(OTPPolicy.DEFAULT_IS_REUSABLE));
    }

    @Test
    public void customValues() throws IOException {
        try (RealmAttributeUpdater ignored = new RealmAttributeUpdater(testRealm())
                .setOtpPolicyAlgorithm(HmacOTP.HMAC_SHA256)
                .setOtpPolicyDigits(3)
                .setOtpPolicyInitialCounter(2)
                .setOtpPolicyPeriod(42)
                .setOtpPolicyType(OTPCredentialModel.HOTP)
                .setOtpPolicyLookAheadWindow(30)
                .setOtpPolicyCodeReusable(!OTPPolicy.DEFAULT_IS_REUSABLE)
                .update()) {

            RealmRepresentation realm = testRealm().toRepresentation();

            assertOtpPolicy(realm,
                    HmacOTP.HMAC_SHA256,
                    3,
                    2,
                    42,
                    OTPCredentialModel.HOTP,
                    30,
                    !OTPPolicy.DEFAULT_IS_REUSABLE);
        }
    }

    private void assertOtpPolicy(RealmRepresentation realm,
                                 String algorithm,
                                 Integer digits,
                                 Integer initialCounter,
                                 Integer period,
                                 String type,
                                 Integer lookAheadWindow,
                                 Boolean isCodeReusable) {
        assertThat(realm, notNullValue());
        assertThat(realm.getOtpPolicyAlgorithm(), is(algorithm));
        assertThat(realm.getOtpPolicyDigits(), is(digits));
        assertThat(realm.getOtpPolicyInitialCounter(), is(initialCounter));
        assertThat(realm.getOtpPolicyPeriod(), is(period));
        assertThat(realm.getOtpPolicyType(), is(type));
        assertThat(realm.getOtpPolicyLookAheadWindow(), is(lookAheadWindow));
        assertThat(realm.isOtpPolicyCodeReusable(), is(isCodeReusable));
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }
}
