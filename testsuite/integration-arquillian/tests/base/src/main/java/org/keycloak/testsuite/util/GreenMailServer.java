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

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.jboss.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.icegreen.greenmail.util.ServerSetup.PROTOCOL_SMTP;
import static com.icegreen.greenmail.util.ServerSetup.PROTOCOL_SMTPS;
import static org.keycloak.testsuite.util.MailServerConfiguration.FROM;
import static org.keycloak.testsuite.util.MailServerConfiguration.HOST;
import static org.keycloak.testsuite.util.MailServerConfiguration.PORT;
import static org.keycloak.testsuite.util.MailServerConfiguration.PORT_SSL;
import static org.keycloak.testsuite.util.MailServerConfiguration.STARTTLS;

public class GreenMailServer {

    private static final Logger log = Logger.getLogger(GreenMailServer.class);
    private static final Map<String, String> serverConfiguration = new HashMap<>();

    public static final String PRIVATE_KEY = "keystore/keycloak.jks";
    public static final String TRUSTED_CERTIFICATE = "keystore/keycloak.truststore";
    public static final String INVALID_KEY = "keystore/email_invalid.jks";

    private GreenMail greenMail;

    private final int port;
    private final String host;

    public GreenMailServer() {
        this(false);
    }

    public GreenMailServer(boolean secure) {
        this(Integer.parseInt(secure ? PORT_SSL : PORT), HOST);
    }

    public GreenMailServer(int port, String host) {
        this.port = port;
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public void start() {
        ServerSetup setup = new ServerSetup(port, host, PROTOCOL_SMTP);

        greenMail = new GreenMail(setup);
        greenMail.start();

        log.info("Started mail server (" + host + ":" + port + ")");
    }

    public void stop() {
        if (greenMail != null) {
            // Suppress error from GreenMail on shutdown
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    if (!(e.getCause() instanceof SocketException && t.getClass().getName()
                            .equals("com.icegreen.greenmail.smtp.SmtpHandler"))) {
                        System.err.print("Exception in thread \"" + t.getName() + "\" ");
                        e.printStackTrace(System.err);
                    }
                }
            });

            greenMail.stop();
        }
    }

    public void startWithSsl(String privateKey) {
        InputStream keyStoreIS;
        try {
            keyStoreIS = new FileInputStream(privateKey);
            char[] keyStorePassphrase = "secret".toCharArray();
            KeyStore ksKeys;
            ksKeys = KeyStore.getInstance("JKS");
            ksKeys.load(keyStoreIS, keyStorePassphrase);

            // KeyManager decides which key material to use.
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ksKeys, keyStorePassphrase);

            // Trust store for client authentication.
            InputStream trustStoreIS = new FileInputStream(String.valueOf(GreenMailServer.class.getClassLoader().getResource(TRUSTED_CERTIFICATE).getFile()));
            char[] trustStorePassphrase = "secret".toCharArray();
            KeyStore ksTrust = KeyStore.getInstance("JKS");
            ksTrust.load(trustStoreIS, trustStorePassphrase);

            // TrustManager decides which certificate authorities to use.
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            tmf.init(ksTrust);

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            ServerSetup setup = new ServerSetup(port, host, PROTOCOL_SMTPS);
            Properties props = new Properties();

            //props.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");
            props.setProperty("mail.smtp.ssl.enable", Boolean.TRUE.toString());
            props.setProperty("mail.smtp.starttls.enable", Boolean.TRUE.toString());
            //props.setProperty("mail.smtp.socketFactory.class", sslContext.getSocketFactory().getClass().getName());
            props.setProperty("mail.smtp.socketFactory.fallback", "false");
            props.setProperty("mail.smtp.ssl.socketFactory", "javax.net.ssl.SSLSocketFactory");
            setup.configureJavaMailSessionProperties(props, true);

            greenMail = new GreenMail(setup);
            greenMail.start();

            log.info("Started mail server with TLS (" + host + ":" + port + ")");
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyManagementException | CertificateException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void credentials(String username, String password) {
        greenMail.setUser(username, password);
    }

    public Map<String, String> getServerConfiguration() {
        serverConfiguration.put("from", FROM);
        serverConfiguration.put("host", host);
        serverConfiguration.put("port", String.valueOf(port));
        serverConfiguration.put("starttls", STARTTLS);
        return serverConfiguration;
    }

    public MimeMessage[] getReceivedMessages() {
        return greenMail.getReceivedMessages();
    }

    /**
     * Returns the very last received message. When no message is available, returns {@code null}.
     *
     * @return see description
     */
    public MimeMessage getLastReceivedMessage() {
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        return (receivedMessages == null || receivedMessages.length == 0)
                ? null
                : receivedMessages[receivedMessages.length - 1];
    }

    /**
     * Use this method if you are sending email in a different thread from the one you're testing from.
     * Block waits for an email to arrive in any mailbox for any user.
     * Implementation Detail: No polling wait implementation
     *
     * @param timeout    maximum time in ms to wait for emailCount of messages to arrive before giving up and returning false
     * @param emailCount waits for these many emails to arrive before returning
     * @return
     */
    public boolean waitForIncomingEmail(long timeout, int emailCount) {
        return greenMail.waitForIncomingEmail(timeout, emailCount);
    }

    /**
     * Does the same thing as Object.wait(long, int) but with a timeout of 5000ms.
     *
     * @param emailCount waits for these many emails to arrive before returning
     * @return
     */
    public boolean waitForIncomingEmail(int emailCount) {
        return greenMail.waitForIncomingEmail(emailCount);
    }
}