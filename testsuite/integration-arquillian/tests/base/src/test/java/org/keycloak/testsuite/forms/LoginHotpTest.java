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
package org.keycloak.testsuite.forms;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.events.Details;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.util.GreenMailExtension;
import org.keycloak.testsuite.util.RealmRepUtil;
import org.keycloak.testsuite.util.UserBuilder;

import java.net.MalformedURLException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
@ExtendWith(GreenMailExtension.class)
public class LoginHotpTest extends AbstractTestRealmKeycloakTest {

    public static OTPPolicy policy;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setOtpPolicyType(OTPCredentialModel.HOTP);
        testRealm.setOtpPolicyAlgorithm(HmacOTP.DEFAULT_ALGORITHM);
        testRealm.setOtpPolicyLookAheadWindow(2);
        testRealm.setOtpPolicyDigits(6);
        UserRepresentation user = RealmRepUtil.findUser(testRealm, "test-user@localhost");
        UserBuilder.edit(user)
                   .hotpSecret("hotpSecret")
                   .otpEnabled();
    }

    

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginTotpPage loginTotpPage;

    private HmacOTP otp; // = new HmacOTP(policy.getDigits(), policy.getAlgorithm(), policy.getLookAheadWindow());

    private int lifespan;

    private static int counter = 0;

    @BeforeEach
    public void before() throws MalformedURLException {
        RealmRepresentation testRealm = testRealm().toRepresentation();

        policy = new OTPPolicy();
        policy.setAlgorithm(testRealm.getOtpPolicyAlgorithm());
        policy.setDigits(testRealm.getOtpPolicyDigits());
        policy.setInitialCounter(testRealm.getOtpPolicyInitialCounter());
        policy.setLookAheadWindow(testRealm.getOtpPolicyLookAheadWindow());
        policy.setPeriod(testRealm.getOtpPolicyLookAheadWindow());
        policy.setType(testRealm.getOtpPolicyType());

        otp = new HmacOTP(policy.getDigits(), policy.getAlgorithm(), policy.getLookAheadWindow());
    }

    @Test
    public void loginWithHotpFailure() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assertions.assertTrue(loginTotpPage.isCurrent());

        loginTotpPage.login("123456");
        loginTotpPage.assertCurrent();
        Assertions.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());

        //loginPage.assertCurrent();  // Invalid authenticator code.
        //Assertions.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().error("invalid_user_credentials").session((String) null)
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    public void loginWithMissingHotp() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assertions.assertTrue(loginTotpPage.isCurrent());

        loginTotpPage.login(null);
        loginTotpPage.assertCurrent();
        Assertions.assertEquals("Invalid authenticator code.", loginTotpPage.getInputError());

        //loginPage.assertCurrent();  // Invalid authenticator code.
        //Assertions.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().error("invalid_user_credentials").session((String) null)
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    public void loginWithHotpSuccess() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assertions.assertTrue("expecting totpPage got: " + driver.getCurrentUrl(), loginTotpPage.isCurrent());

        loginTotpPage.login(otp.generateHOTP("hotpSecret", counter++));

        appPage.assertCurrent();

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();
    }

    @Test
    public void loginWithHotpInvalidPassword() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "invalid");

        Assertions.assertTrue(loginPage.isCurrent());

        Assertions.assertEquals("Invalid username or password.", loginPage.getInputError());

        events.expectLogin().error("invalid_user_credentials").session((String) null)
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }
}
