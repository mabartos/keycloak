/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.adapters.saml.config.parsers;

import org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames;
import org.keycloak.saml.processing.core.parsers.util.HasQName;

import javax.xml.namespace.QName;

/**
 * @author hmlnarik
 */
public enum KeycloakSamlAdapterV1QNames implements HasQName {

    ALLOWED_CLOCK_SKEW(KeycloakSamlAdapterNames.ALLOWED_CLOCK_SKEW),
    ATTRIBUTE(KeycloakSamlAdapterNames.ATTRIBUTE),
    CERTIFICATE(KeycloakSamlAdapterNames.CERTIFICATE),
    CERTIFICATE_PEM(KeycloakSamlAdapterNames.CERTIFICATE_PEM),
    HTTP_CLIENT(KeycloakSamlAdapterNames.HTTP_CLIENT),
    IDP(KeycloakSamlAdapterNames.IDP),
    KEY(KeycloakSamlAdapterNames.KEY),
    KEYCLOAK_SAML_ADAPTER(KeycloakSamlAdapterNames.KEYCLOAK_SAML_ADAPTER),
    KEYS(KeycloakSamlAdapterNames.KEYS),
    KEY_STORE(KeycloakSamlAdapterNames.KEY_STORE),
    PRINCIPAL_NAME_MAPPING(KeycloakSamlAdapterNames.PRINCIPAL_NAME_MAPPING),
    PRIVATE_KEY(KeycloakSamlAdapterNames.PRIVATE_KEY),
    PRIVATE_KEY_PEM(KeycloakSamlAdapterNames.PRIVATE_KEY_PEM),
    PROPERTY(KeycloakSamlAdapterNames.PROPERTY),
    PUBLIC_KEY_PEM(KeycloakSamlAdapterNames.PUBLIC_KEY_PEM),
    ROLE_IDENTIFIERS(KeycloakSamlAdapterNames.ROLE_IDENTIFIERS),
    ROLE_MAPPINGS_PROVIDER(KeycloakSamlAdapterNames.ROLE_MAPPINGS_PROVIDER),
    SINGLE_LOGOUT_SERVICE(KeycloakSamlAdapterNames.SINGLE_LOGOUT_SERVICE),
    SINGLE_SIGN_ON_SERVICE(KeycloakSamlAdapterNames.SINGLE_SIGN_ON_SERVICE),
    SP(KeycloakSamlAdapterNames.SP),

    ATTR_ALIAS(null, KeycloakSamlAdapterNames.ATTR_ALIAS),
    ATTR_ALLOW_ANY_HOSTNAME(null, KeycloakSamlAdapterNames.ATTR_ALLOW_ANY_HOSTNAME),
    ATTR_ASSERTION_CONSUMER_SERVICE_URL(null, KeycloakSamlAdapterNames.ATTR_ASSERTION_CONSUMER_SERVICE_URL),
    ATTR_ATTRIBUTE(null, KeycloakSamlAdapterNames.ATTR_ATTRIBUTE),
    ATTR_AUTODETECT_BEARER_ONLY(null, KeycloakSamlAdapterNames.ATTR_AUTODETECT_BEARER_ONLY),
    ATTR_BINDING_URL(null, KeycloakSamlAdapterNames.ATTR_BINDING_URL),
    ATTR_CLIENT_KEYSTORE(null, KeycloakSamlAdapterNames.ATTR_CLIENT_KEYSTORE),
    ATTR_CLIENT_KEYSTORE_PASSWORD(null, KeycloakSamlAdapterNames.ATTR_CLIENT_KEYSTORE_PASSWORD),
    ATTR_CONNECTION_POOL_SIZE(null, KeycloakSamlAdapterNames.ATTR_CONNECTION_POOL_SIZE),
    ATTR_DISABLE_TRUST_MANAGER(null, KeycloakSamlAdapterNames.ATTR_DISABLE_TRUST_MANAGER),
    ATTR_ENCRYPTION(null, KeycloakSamlAdapterNames.ATTR_ENCRYPTION),
    ATTR_ENTITY_ID(null, KeycloakSamlAdapterNames.ATTR_ENTITY_ID),
    ATTR_FILE(null, KeycloakSamlAdapterNames.ATTR_FILE),
    ATTR_FORCE_AUTHENTICATION(null, KeycloakSamlAdapterNames.ATTR_FORCE_AUTHENTICATION),
    ATTR_ID(null, KeycloakSamlAdapterNames.ATTR_ID),
    ATTR_IS_PASSIVE(null, KeycloakSamlAdapterNames.ATTR_IS_PASSIVE),
    ATTR_LOGOUT_PAGE(null, KeycloakSamlAdapterNames.ATTR_LOGOUT_PAGE),
    ATTR_METADATA_URL(null, KeycloakSamlAdapterNames.ATTR_METADATA_URL),
    ATTR_NAME(null, KeycloakSamlAdapterNames.ATTR_NAME),
    ATTR_NAME_ID_POLICY_FORMAT(null, KeycloakSamlAdapterNames.ATTR_NAME_ID_POLICY_FORMAT),
    ATTR_PASSWORD(null, KeycloakSamlAdapterNames.ATTR_PASSWORD),
    ATTR_POLICY(null, KeycloakSamlAdapterNames.ATTR_POLICY),
    ATTR_POST_BINDING_URL(null, KeycloakSamlAdapterNames.ATTR_POST_BINDING_URL),
    ATTR_PROXY_URL(null, KeycloakSamlAdapterNames.ATTR_PROXY_URL),
    ATTR_REDIRECT_BINDING_URL(null, KeycloakSamlAdapterNames.ATTR_REDIRECT_BINDING_URL),
    ATTR_REQUEST_BINDING(null, KeycloakSamlAdapterNames.ATTR_REQUEST_BINDING),
    ATTR_RESOURCE(null, KeycloakSamlAdapterNames.ATTR_RESOURCE),
    ATTR_RESPONSE_BINDING(null, KeycloakSamlAdapterNames.ATTR_RESPONSE_BINDING),
    ATTR_SIGNATURES_REQUIRED(null, KeycloakSamlAdapterNames.ATTR_SIGNATURES_REQUIRED),
    ATTR_SIGNATURE_ALGORITHM(null, KeycloakSamlAdapterNames.ATTR_SIGNATURE_ALGORITHM),
    ATTR_SIGNATURE_CANONICALIZATION_METHOD(null, KeycloakSamlAdapterNames.ATTR_SIGNATURE_CANONICALIZATION_METHOD),
    ATTR_SIGNING(null, KeycloakSamlAdapterNames.ATTR_SIGNING),
    ATTR_SIGN_REQUEST(null, KeycloakSamlAdapterNames.ATTR_SIGN_REQUEST),
    ATTR_SIGN_RESPONSE(null, KeycloakSamlAdapterNames.ATTR_SIGN_RESPONSE),
    ATTR_SSL_POLICY(null, KeycloakSamlAdapterNames.ATTR_SSL_POLICY),
    ATTR_TRUSTSTORE(null, KeycloakSamlAdapterNames.ATTR_TRUSTSTORE),
    ATTR_TRUSTSTORE_PASSWORD(null, KeycloakSamlAdapterNames.ATTR_TRUSTSTORE_PASSWORD),
    ATTR_TURN_OFF_CHANGE_SESSSION_ID_ON_LOGIN(null, KeycloakSamlAdapterNames.ATTR_TURN_OFF_CHANGE_SESSSION_ID_ON_LOGIN),
    ATTR_TYPE(null, KeycloakSamlAdapterNames.ATTR_TYPE),
    ATTR_UNIT(null, KeycloakSamlAdapterNames.ATTR_UNIT),
    ATTR_VALIDATE_ASSERTION_SIGNATURE(null, KeycloakSamlAdapterNames.ATTR_VALIDATE_ASSERTION_SIGNATURE),
    ATTR_VALIDATE_REQUEST_SIGNATURE(null, KeycloakSamlAdapterNames.ATTR_VALIDATE_REQUEST_SIGNATURE),
    ATTR_VALIDATE_RESPONSE_SIGNATURE(null, KeycloakSamlAdapterNames.ATTR_VALIDATE_RESPONSE_SIGNATURE),
    ATTR_VALUE(null, KeycloakSamlAdapterNames.ATTR_VALUE),
    ATTR_KEEP_DOM_ASSERTION(null, KeycloakSamlAdapterNames.ATTR_KEEP_DOM_ASSERTION),
    ATTR_SOCKET_TIMEOUT(null, KeycloakSamlAdapterNames.ATTR_SOCKET_TIMEOUT),
    ATTR_CONNECTION_TIMEOUT(null, KeycloakSamlAdapterNames.ATTR_CONNECTION_TIMEOUT),
    ATTR_CONNECTION_TTL(null, KeycloakSamlAdapterNames.ATTR_CONNECTION_TTL),

    XMLNS(KeycloakSamlAdapterNames.XMLNS),
    XMLNS_XSI(KeycloakSamlAdapterNames.XMLNS_XSI),
    XSI_SCHEMA_LOCATION(KeycloakSamlAdapterNames.XSI_SCHEMA_LOCATION),

    UNKNOWN_ELEMENT(KeycloakSamlAdapterNames.UNKNOWN_ELEMENT);

    public static final String NS_URI = "urn:keycloak:saml:adapter";
    public static final String XMLNS_XSI_DEFAULT = "http://www.w3.org/2001/XMLSchema-instance";

    private final QName qName;

    KeycloakSamlAdapterV1QNames(String localName) {
        this(NS_URI, localName);
    }

    KeycloakSamlAdapterV1QNames(HasQName source) {
        this.qName = source.getQName();
    }

    KeycloakSamlAdapterV1QNames(String nsUri, String localName) {
        this.qName = new QName(nsUri, localName);
    }

    @Override
    public QName getQName() {
        return qName;
    }

    public QName getQName(String prefix) {
        return new QName(this.qName.getNamespaceURI(), this.qName.getLocalPart(), prefix);
    }
}
