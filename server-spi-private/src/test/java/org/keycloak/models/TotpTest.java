/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.keycloak.models.utils.TimeBasedOTP;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class TotpTest {

    @Test
    public void testTotp() {

        TimeBasedOTP totp = new TimeBasedOTP("HmacSHA1", 8, 30, 1);
        String secret = "dSdmuHLQhkm54oIm0A0S";
        String otp = totp.generateTOTP(secret);

        assertTrue(totp.validateTOTP(otp, secret.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * KEYCLOAK-18880
     */
    @Test
    public void testTotpLookAround() {

        int lookAroundWindow = 2;
        TimeBasedOTP totp = new TimeBasedOTP("HmacSHA1", 8, 60, lookAroundWindow);
        String secret = "dSdmuHLQhkm54oIm0A0S";
        String otp = totp.generateTOTP(secret);

        for (int i = -lookAroundWindow; i <= lookAroundWindow; i++) {

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, i);
            totp.setCalendar(calendar);

            assertTrue("Should accept code with skew offset " + i,totp.validateTOTP(otp, secret.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    public void testTotpReuse() {
        final int timeInterval = 2;
        long lastSuccessValidationInterval = 0;
        final int time = 20;

        int tmpInterval = timeInterval;

        final TimeBasedOTP totp = new TimeBasedOTP("HmacSHA1", 8, timeInterval, 2);
        final Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        totp.setCalendar(calendar);

        final String secret = "dSdmuHLQhkm54oIm0A0S";
        String otp = totp.generateTOTP(secret);

        for (int i = 0; i < time; i++) {
            boolean valid = totp.validateTOTP(otp, secret.getBytes(StandardCharsets.UTF_8), lastSuccessValidationInterval);
            assertThat(valid, CoreMatchers.is(i % 3 == 0));

            if (valid) {
                lastSuccessValidationInterval = totp.getValidationInterval();
            }

            calendar.add(Calendar.SECOND, 1);

            if (tmpInterval == 0) {
                otp = totp.generateTOTP(secret);
                tmpInterval = timeInterval;
            } else {
                tmpInterval--;
            }
        }
    }

    @Test
    public void totpReuseMultipleCodes() {
        final int timeInterval = 30;
        final int lookAroundWindow = 2;

        final TimeBasedOTP totp = new TimeBasedOTP("HmacSHA1", 8, timeInterval, lookAroundWindow);
        final String secret = "dSdmuHLQhkm54oIm0A0S";

        final Calendar initialCalendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        final Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        totp.setCalendar(calendar);

        // Initialize OTP codes
        String[] otps = new String[5];

        // current window
        otps[2] = totp.generateTOTP(secret);

        calendar.add(Calendar.SECOND, -timeInterval);
        // current window - 1
        otps[1] = totp.generateTOTP(secret);

        calendar.add(Calendar.SECOND, -timeInterval);
        // current window - 2
        otps[0] = totp.generateTOTP(secret);

        calendar.add(Calendar.SECOND, 2 * timeInterval);

        calendar.add(Calendar.SECOND, timeInterval);
        // current window + 1
        otps[3] = totp.generateTOTP(secret);

        calendar.add(Calendar.SECOND, timeInterval);
        // current window + 2
        otps[4] = totp.generateTOTP(secret);

        // Set calendar to the initial one
        totp.setCalendar(initialCalendar);

        final AtomicLong lastSuccessValidationInterval = new AtomicLong(0L);

        // Assert validity of the OTP code
        BiConsumer<String, Boolean> assertValidOtp = (otp, expectedValue) -> {
            boolean valid = totp.validateTOTP(otp, secret.getBytes(StandardCharsets.UTF_8), lastSuccessValidationInterval.get());
            assertThat(valid, CoreMatchers.is(expectedValue));
            if (valid) {
                lastSuccessValidationInterval.set(totp.getValidationInterval());
            }
        };

        // Use the currentWindow - 2 OTP
        assertValidOtp.accept(otps[0], true);

        // Use the currentWindow OTP
        assertValidOtp.accept(otps[2], true);

        // Use the currentWindow + 1 OTP
        assertValidOtp.accept(otps[1], false);

        // Use the currentWindow + 2 OTP
        assertValidOtp.accept(otps[4], true);

        // Use the currentWindow + 2 OTP
        assertValidOtp.accept(otps[4], false);
    }
}
