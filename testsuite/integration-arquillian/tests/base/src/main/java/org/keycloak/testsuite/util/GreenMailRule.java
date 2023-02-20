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

package org.keycloak.testsuite.util;

import jakarta.mail.internet.MimeMessage;
import org.junit.rules.ExternalResource;
import org.keycloak.models.RealmModel;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GreenMailRule extends ExternalResource {

    private final GreenMailServer greenMail;

    public GreenMailRule() {
        this(3025, "localhost");
    }

    public GreenMailRule(int port, String host) {
        this.greenMail = new GreenMailServer(port, host);
    }

    @Override
    protected void before() throws Throwable {
        greenMail.start();
    }

    public void credentials(String username, String password) {
        greenMail.credentials(username, password);
    }

    @Override
    protected void after() {
        greenMail.stop();
    }

    public void configureRealm(RealmModel realm) {
        Map<String, String> config = new HashMap<>();
        config.put("from", "auto@keycloak.org");
        config.put("host", greenMail.getHost());
        config.put("port", String.valueOf(greenMail.getPort()));
        realm.setSmtpConfig(config);
    }

    public MimeMessage[] getReceivedMessages() {
        return greenMail.getReceivedMessages();
    }

    public MimeMessage getLastReceivedMessage() {
        return greenMail.getLastReceivedMessage();
    }

    public boolean waitForIncomingEmail(long timeout, int emailCount) {
        return greenMail.waitForIncomingEmail(timeout, emailCount);
    }

    public boolean waitForIncomingEmail(int emailCount) {
        return greenMail.waitForIncomingEmail(emailCount);
    }
}
