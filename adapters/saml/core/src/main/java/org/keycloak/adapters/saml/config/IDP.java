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

import org.keycloak.adapters.cloned.AdapterHttpClientConfig;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ALLOWED_CLOCK_SKEW;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_ALLOW_ANY_HOSTNAME;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_ASSERTION_CONSUMER_SERVICE_URL;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_BINDING_URL;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_CLIENT_KEYSTORE;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_CLIENT_KEYSTORE_PASSWORD;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_CONNECTION_POOL_SIZE;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_CONNECTION_TIMEOUT;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_CONNECTION_TTL;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_DISABLE_TRUST_MANAGER;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_ENTITY_ID;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_METADATA_URL;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_POST_BINDING_URL;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_PROXY_URL;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_REDIRECT_BINDING_URL;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_REQUEST_BINDING;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_RESPONSE_BINDING;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_SIGNATURES_REQUIRED;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_SIGNATURE_ALGORITHM;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_SIGNATURE_CANONICALIZATION_METHOD;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_SIGN_REQUEST;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_SIGN_RESPONSE;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_SOCKET_TIMEOUT;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_TRUSTSTORE;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_TRUSTSTORE_PASSWORD;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_UNIT;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_VALIDATE_ASSERTION_SIGNATURE;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_VALIDATE_REQUEST_SIGNATURE;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_VALIDATE_RESPONSE_SIGNATURE;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.HTTP_CLIENT;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.KEY;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.KEYS;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.SINGLE_LOGOUT_SERVICE;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.SINGLE_SIGN_ON_SERVICE;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@XmlRootElement(name = KeycloakSamlAdapterNames.IDP)
@XmlAccessorType(XmlAccessType.FIELD)
public class IDP implements Serializable {

    @XmlRootElement(name = SINGLE_SIGN_ON_SERVICE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SingleSignOnService implements Serializable {
        @XmlAttribute(name = ATTR_SIGN_REQUEST)
        private Boolean signRequest;

        @XmlAttribute(name = ATTR_VALIDATE_RESPONSE_SIGNATURE)
        private Boolean validateResponseSignature;

        @XmlAttribute(name = ATTR_REQUEST_BINDING)
        private String requestBinding;

        @XmlAttribute(name = ATTR_RESPONSE_BINDING)
        private String responseBinding;

        @XmlAttribute(name = ATTR_BINDING_URL)
        private String bindingUrl;

        @XmlAttribute(name = ATTR_ASSERTION_CONSUMER_SERVICE_URL)
        private String assertionConsumerServiceUrl;

        @XmlAttribute(name = ATTR_VALIDATE_ASSERTION_SIGNATURE)
        private Boolean validateAssertionSignature;

        @XmlAttribute(name = ATTR_SIGNATURES_REQUIRED)
        private boolean signaturesRequired = false;

        public boolean isSignRequest() {
            return signRequest == null ? signaturesRequired : signRequest;
        }

        public void setSignRequest(Boolean signRequest) {
            this.signRequest = signRequest;
        }

        public boolean isValidateResponseSignature() {
            return validateResponseSignature == null ? signaturesRequired : validateResponseSignature;
        }

        public void setValidateResponseSignature(Boolean validateResponseSignature) {
            this.validateResponseSignature = validateResponseSignature;
        }

        public boolean isValidateAssertionSignature() {
            return validateAssertionSignature == null ? false : validateAssertionSignature;
        }

        public void setValidateAssertionSignature(Boolean validateAssertionSignature) {
            this.validateAssertionSignature = validateAssertionSignature;
        }

        public String getRequestBinding() {
            return requestBinding;
        }

        public void setRequestBinding(String requestBinding) {
            this.requestBinding = requestBinding;
        }

        public String getResponseBinding() {
            return responseBinding;
        }

        public void setResponseBinding(String responseBinding) {
            this.responseBinding = responseBinding;
        }

        public String getBindingUrl() {
            return bindingUrl;
        }

        public void setBindingUrl(String bindingUrl) {
            this.bindingUrl = bindingUrl;
        }

        public String getAssertionConsumerServiceUrl() {
            return assertionConsumerServiceUrl;
        }

        public void setAssertionConsumerServiceUrl(String assertionConsumerServiceUrl) {
            this.assertionConsumerServiceUrl = assertionConsumerServiceUrl;
        }

        private void setSignaturesRequired(boolean signaturesRequired) {
            this.signaturesRequired = signaturesRequired;
        }
    }

    @XmlRootElement(name = SINGLE_LOGOUT_SERVICE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SingleLogoutService implements Serializable {
        @XmlAttribute(name = ATTR_SIGN_REQUEST)
        private Boolean signRequest;

        @XmlAttribute(name = ATTR_SIGN_RESPONSE)
        private Boolean signResponse;

        @XmlAttribute(name = ATTR_VALIDATE_REQUEST_SIGNATURE)
        private Boolean validateRequestSignature;

        @XmlAttribute(name = ATTR_VALIDATE_RESPONSE_SIGNATURE)
        private Boolean validateResponseSignature;

        @XmlAttribute(name = ATTR_REQUEST_BINDING)
        private String requestBinding;

        @XmlAttribute(name = ATTR_RESPONSE_BINDING)
        private String responseBinding;

        @XmlAttribute(name = ATTR_POST_BINDING_URL)
        private String postBindingUrl;

        @XmlAttribute(name = ATTR_REDIRECT_BINDING_URL)
        private String redirectBindingUrl;

        @XmlAttribute(name = ATTR_SIGNATURES_REQUIRED)
        private boolean signaturesRequired = false;

        public boolean isSignRequest() {
            return signRequest == null ? signaturesRequired : signRequest;
        }

        public void setSignRequest(Boolean signRequest) {
            this.signRequest = signRequest;
        }

        public boolean isSignResponse() {
            return signResponse == null ? signaturesRequired : signResponse;
        }

        public void setSignResponse(Boolean signResponse) {
            this.signResponse = signResponse;
        }

        public boolean isValidateRequestSignature() {
            return validateRequestSignature == null ? signaturesRequired : validateRequestSignature;
        }

        public void setValidateRequestSignature(Boolean validateRequestSignature) {
            this.validateRequestSignature = validateRequestSignature;
        }

        public boolean isValidateResponseSignature() {
            return validateResponseSignature == null ? signaturesRequired : validateResponseSignature;
        }

        public void setValidateResponseSignature(Boolean validateResponseSignature) {
            this.validateResponseSignature = validateResponseSignature;
        }

        public String getRequestBinding() {
            return requestBinding;
        }

        public void setRequestBinding(String requestBinding) {
            this.requestBinding = requestBinding;
        }

        public String getResponseBinding() {
            return responseBinding;
        }

        public void setResponseBinding(String responseBinding) {
            this.responseBinding = responseBinding;
        }

        public String getPostBindingUrl() {
            return postBindingUrl;
        }

        public void setPostBindingUrl(String postBindingUrl) {
            this.postBindingUrl = postBindingUrl;
        }

        public String getRedirectBindingUrl() {
            return redirectBindingUrl;
        }

        public void setRedirectBindingUrl(String redirectBindingUrl) {
            this.redirectBindingUrl = redirectBindingUrl;
        }

        private void setSignaturesRequired(boolean signaturesRequired) {
            this.signaturesRequired = signaturesRequired;
        }
    }

    @XmlRootElement(name = HTTP_CLIENT)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class HttpClientConfig implements AdapterHttpClientConfig {
        @XmlAttribute(name = ATTR_TRUSTSTORE)
        private String truststore;

        @XmlAttribute(name = ATTR_TRUSTSTORE_PASSWORD)
        private String truststorePassword;

        @XmlAttribute(name = ATTR_CLIENT_KEYSTORE)
        private String clientKeystore;

        @XmlAttribute(name = ATTR_CLIENT_KEYSTORE_PASSWORD)
        private String clientKeystorePassword;

        @XmlAttribute(name = ATTR_ALLOW_ANY_HOSTNAME)
        private boolean allowAnyHostname;

        @XmlAttribute(name = ATTR_DISABLE_TRUST_MANAGER)
        private boolean disableTrustManager;

        @XmlAttribute(name = ATTR_CONNECTION_POOL_SIZE)
        private int connectionPoolSize;

        @XmlAttribute(name = ATTR_PROXY_URL)
        private String proxyUrl;

        @XmlAttribute(name = ATTR_SOCKET_TIMEOUT)
        private long socketTimeout;

        @XmlAttribute(name = ATTR_CONNECTION_TIMEOUT)
        private long connectionTimeout;

        @XmlAttribute(name = ATTR_CONNECTION_TTL)
        private long connectionTtl;

        @Override
        public String getTruststore() {
            return truststore;
        }

        public void setTruststore(String truststore) {
            this.truststore = truststore;
        }

        @Override
        public String getTruststorePassword() {
            return truststorePassword;
        }

        public void setTruststorePassword(String truststorePassword) {
            this.truststorePassword = truststorePassword;
        }

        @Override
        public String getClientKeystore() {
            return clientKeystore;
        }

        public void setClientKeystore(String clientKeystore) {
            this.clientKeystore = clientKeystore;
        }

        @Override
        public String getClientKeystorePassword() {
            return clientKeystorePassword;
        }

        public void setClientKeystorePassword(String clientKeystorePassword) {
            this.clientKeystorePassword = clientKeystorePassword;
        }

        @Override
        public boolean isAllowAnyHostname() {
            return allowAnyHostname;
        }

        public void setAllowAnyHostname(boolean allowAnyHostname) {
            this.allowAnyHostname = allowAnyHostname;
        }

        @Override
        public boolean isDisableTrustManager() {
            return disableTrustManager;
        }

        public void setDisableTrustManager(boolean disableTrustManager) {
            this.disableTrustManager = disableTrustManager;
        }

        @Override
        public int getConnectionPoolSize() {
            return connectionPoolSize;
        }

        public void setConnectionPoolSize(int connectionPoolSize) {
            this.connectionPoolSize = connectionPoolSize;
        }

        @Override
        public String getProxyUrl() {
            return proxyUrl;
        }

        @Override
        public long getSocketTimeout() {
            return socketTimeout;
        }

        public void setSocketTimeout(long socketTimeout) {
            this.socketTimeout = socketTimeout;
        }

        @Override
        public long getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        @Override
        public long getConnectionTTL() {
            return connectionTtl;
        }

        public void setConnectionTTL(long connectionTtl) {
            this.connectionTtl = connectionTtl;
        }

        public void setProxyUrl(String proxyUrl) {
            this.proxyUrl = proxyUrl;
        }
    }

    @XmlRootElement(name = ALLOWED_CLOCK_SKEW)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AllowedClockSkew {
        @XmlAttribute(name = ATTR_UNIT)
        private String unit;

        @XmlValue
        private Integer value;

        public TimeUnit getUnit() {
            return TimeUnit.valueOf(unit);
        }

        public void setUnit(TimeUnit unit) {
            this.unit = unit.toString();
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }
    }

    @XmlAttribute(name = ATTR_ENTITY_ID)
    private String entityID;

    @XmlAttribute(name = ATTR_SIGNATURE_ALGORITHM)
    private String signatureAlgorithm;

    @XmlAttribute(name = ATTR_SIGNATURE_CANONICALIZATION_METHOD)
    private String signatureCanonicalizationMethod;

    @XmlElement(name = SINGLE_SIGN_ON_SERVICE)
    private SingleSignOnService singleSignOnService;

    @XmlElement(name = SINGLE_LOGOUT_SERVICE)
    private SingleLogoutService singleLogoutService;

    @XmlElementWrapper(name = KEYS)
    @XmlElement(name = KEY)
    private List<Key> keys;

    @XmlElement(name = HTTP_CLIENT, type = HttpClientConfig.class)
    private AdapterHttpClientConfig httpClientConfig = new HttpClientConfig();

    @XmlAttribute(name = ATTR_SIGNATURES_REQUIRED)
    private boolean signaturesRequired = false;

    @XmlAttribute(name = ATTR_METADATA_URL)
    private String metadataUrl;

    @XmlElement(name = ALLOWED_CLOCK_SKEW)
    private AllowedClockSkew allowedClockSkew = new AllowedClockSkew();

    @XmlElement(name = ATTR_UNIT)
    private TimeUnit allowedClockSkewUnit;

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(String entityID) {
        this.entityID = entityID;
    }

    public SingleSignOnService getSingleSignOnService() {
        return singleSignOnService;
    }

    public void setSingleSignOnService(SingleSignOnService singleSignOnService) {
        this.singleSignOnService = singleSignOnService;
        if (singleSignOnService != null) {
            singleSignOnService.setSignaturesRequired(signaturesRequired);
        }
    }

    public SingleLogoutService getSingleLogoutService() {
        return singleLogoutService;
    }

    public void setSingleLogoutService(SingleLogoutService singleLogoutService) {
        this.singleLogoutService = singleLogoutService;
        if (singleLogoutService != null) {
            singleLogoutService.setSignaturesRequired(signaturesRequired);
        }
    }

    public List<Key> getKeys() {
        return keys;
    }

    public void setKeys(List<Key> keys) {
        this.keys = keys;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getSignatureCanonicalizationMethod() {
        return signatureCanonicalizationMethod;
    }

    public void setSignatureCanonicalizationMethod(String signatureCanonicalizationMethod) {
        this.signatureCanonicalizationMethod = signatureCanonicalizationMethod;
    }

    public AdapterHttpClientConfig getHttpClientConfig() {
        return httpClientConfig;
    }

    public void setHttpClientConfig(AdapterHttpClientConfig httpClientConfig) {
        this.httpClientConfig = httpClientConfig;
    }

    public boolean isSignaturesRequired() {
        return signaturesRequired;
    }

    public void setSignaturesRequired(boolean signaturesRequired) {
        this.signaturesRequired = signaturesRequired;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    public Integer getAllowedClockSkew() {
        return allowedClockSkew.getValue();
    }

    public void setAllowedClockSkew(Integer allowedClockSkew) {
        this.allowedClockSkew.setValue(allowedClockSkew);
    }

    public TimeUnit getAllowedClockSkewUnit() {
        return allowedClockSkew.getUnit();
    }

    public void setAllowedClockSkewUnit(TimeUnit allowedClockSkewUnit) {
        allowedClockSkew.setUnit(allowedClockSkewUnit);
    }
}
