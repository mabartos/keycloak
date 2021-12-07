/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.forms.login.freemarker.model;

import org.keycloak.common.util.Base64Url;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class WebAuthnAuthenticatorsBean {
    private List<WebAuthnAuthenticatorBean> authenticators;

    public WebAuthnAuthenticatorsBean(KeycloakSession session, RealmModel realm, UserModel user, String credentialType) {
        // should consider multiple credentials in the future, but only single credential supported now.
        this.authenticators = session.userCredentialManager().getStoredCredentialsByTypeStream(realm, user, credentialType)
                .map(WebAuthnCredentialModel::createFromCredentialModel)
                .map(webAuthnCredential -> {
                    String credentialId = Base64Url.encodeBase64ToBase64Url(webAuthnCredential.getWebAuthnCredentialData().getCredentialId());
                    String label = (webAuthnCredential.getUserLabel() == null || webAuthnCredential.getUserLabel().isEmpty()) ? "label missing" : webAuthnCredential.getUserLabel();
                    String createdAt = getDateTimeFromMillis(webAuthnCredential.getCreatedDate());
                    return new WebAuthnAuthenticatorBean(credentialId, label, createdAt);
                }).collect(Collectors.toList());
    }

    private String getDateTimeFromMillis(long millis) {
        return getDateTimeFromMillis(millis, "dd-MM-yyyy HH:mm");
    }

    private String getDateTimeFromMillis(long millis, String format) {
        final LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
        final DateTimeFormatter createdFormat = DateTimeFormatter.ofPattern(format);
        return date.format(createdFormat);
    }


    public List<WebAuthnAuthenticatorBean> getAuthenticators() {
        return authenticators;
    }

    public static class WebAuthnAuthenticatorBean {
        private final String credentialId;
        private final String label;
        private final String createdAt;

        public WebAuthnAuthenticatorBean(String credentialId, String label, String createdAt) {
            this.credentialId = credentialId;
            this.label = label;
            this.createdAt = createdAt;
        }

        public String getCredentialId() {
            return this.credentialId;
        }

        public String getLabel() {
            return this.label;
        }

        public String getCreatedAt() {
            return this.createdAt;
        }
    }
}
