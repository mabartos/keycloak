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

package org.keycloak.adapters.saml.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@XmlRootElement(name = KeycloakSamlAdapterNames.KEY)
@XmlAccessorType(XmlAccessType.FIELD)
public class Key implements Serializable {

    @XmlRootElement(name = KeycloakSamlAdapterNames.KEY_STORE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class KeyStoreConfig implements Serializable {

        @XmlAttribute(name = KeycloakSamlAdapterNames.ATTR_FILE)
        private String file;
        @XmlAttribute(name = KeycloakSamlAdapterNames.ATTR_RESOURCE)
        private String resource;
        @XmlAttribute(name = KeycloakSamlAdapterNames.ATTR_PASSWORD)
        private String password;
        @XmlAttribute(name = KeycloakSamlAdapterNames.ATTR_TYPE)
        private String type;
        @XmlAttribute(name = KeycloakSamlAdapterNames.ATTR_ALIAS)
        private String alias;

        @XmlElement(name = KeycloakSamlAdapterNames.PRIVATE_KEY)
        private final PrivateKey privateKey = new PrivateKey();

        @XmlElement(name = KeycloakSamlAdapterNames.CERTIFICATE)
        private final Certificate certificate = new Certificate();

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPrivateKeyAlias() {
            return privateKey.getAlias();
        }

        public void setPrivateKeyAlias(String privateKeyAlias) {
            privateKey.setAlias(privateKeyAlias);
        }

        public String getPrivateKeyPassword() {
            return privateKey.getPassword();
        }

        public void setPrivateKeyPassword(String privateKeyPassword) {
            privateKey.setPassword(privateKeyPassword);
        }

        public String getCertificateAlias() {
            return certificate.getAlias();
        }

        public void setCertificateAlias(String certificateAlias) {
            certificate.setAlias(certificateAlias);
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        private static class AliasAttribute {

            @XmlAttribute(name = KeycloakSamlAdapterNames.ATTR_ALIAS)
            private String alias;

            public String getAlias() {
                return alias;
            }

            public void setAlias(String alias) {
                this.alias = alias;
            }
        }

        @XmlRootElement(name = KeycloakSamlAdapterNames.PRIVATE_KEY)
        @XmlAccessorType(XmlAccessType.FIELD)
        public static class PrivateKey extends AliasAttribute {

            @XmlAttribute(name = KeycloakSamlAdapterNames.ATTR_PASSWORD)
            private String password;

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }
        }

        @XmlRootElement(name = KeycloakSamlAdapterNames.CERTIFICATE)
        @XmlAccessorType(XmlAccessType.FIELD)
        public static class Certificate extends AliasAttribute {
        }

    }

    @XmlAttribute(name = KeycloakSamlAdapterNames.ATTR_SIGNING)
    private boolean signing;
    @XmlAttribute(name = KeycloakSamlAdapterNames.ATTR_ENCRYPTION)
    private boolean encryption;
    @XmlElement(name = KeycloakSamlAdapterNames.KEY_STORE)
    private KeyStoreConfig keystore;
    @XmlElement(name = KeycloakSamlAdapterNames.PRIVATE_KEY_PEM)
    private String privateKeyPem;
    @XmlElement(name = KeycloakSamlAdapterNames.PUBLIC_KEY_PEM)
    private String publicKeyPem;
    @XmlElement(name = KeycloakSamlAdapterNames.CERTIFICATE_PEM)
    private String certificatePem;

    public boolean isSigning() {
        return signing;
    }

    public void setSigning(Boolean signing) {
        this.signing = signing != null && signing;
    }

    public boolean isEncryption() {
        return encryption;
    }

    public void setEncryption(Boolean encryption) {
        this.encryption = encryption != null && encryption;
    }

    public KeyStoreConfig getKeystore() {
        return keystore;
    }

    public void setKeystore(KeyStoreConfig keystore) {
        this.keystore = keystore;
    }

    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public void setPrivateKeyPem(String privateKeyPem) {
        this.privateKeyPem = privateKeyPem;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }

    public String getCertificatePem() {
        return certificatePem;
    }

    public void setCertificatePem(String certificatePem) {
        this.certificatePem = certificatePem;
    }
}
