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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTRIBUTE;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_ATTRIBUTE;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_AUTODETECT_BEARER_ONLY;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_ENTITY_ID;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_FORCE_AUTHENTICATION;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_ID;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_IS_PASSIVE;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_KEEP_DOM_ASSERTION;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_LOGOUT_PAGE;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_NAME;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_NAME_ID_POLICY_FORMAT;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_POLICY;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_SSL_POLICY;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_TURN_OFF_CHANGE_SESSSION_ID_ON_LOGIN;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ATTR_VALUE;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.KEY;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.KEYS;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.PRINCIPAL_NAME_MAPPING;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.PROPERTY;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ROLE_IDENTIFIERS;
import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.ROLE_MAPPINGS_PROVIDER;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@XmlRootElement(name = KeycloakSamlAdapterNames.SP)
@XmlAccessorType(XmlAccessType.FIELD)
public class SP implements Serializable {

    @XmlRootElement(name = PRINCIPAL_NAME_MAPPING)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PrincipalNameMapping implements Serializable {
        @XmlAttribute(name = ATTR_POLICY)
        private String policy;
        @XmlAttribute(name = ATTR_ATTRIBUTE)
        private String attributeName;

        public String getPolicy() {
            return policy;
        }

        public void setPolicy(String policy) {
            this.policy = policy;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public void setAttributeName(String attributeName) {
            this.attributeName = attributeName;
        }
    }

    /**
     * Holds the configuration of the {@code RoleMappingsProvider}. Contains the provider's id and a {@link Properties}
     * object that holds the provider's configuration options.
     */
    @XmlRootElement(name = ROLE_MAPPINGS_PROVIDER)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RoleMappingsProviderConfig implements Serializable {
        @XmlAttribute(name = ATTR_ID)
        private String id;

        @XmlElement(name = PROPERTY)
        private Set<Property> configuration;

        public String getId() {
            return this.id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public Properties getConfiguration() {
            Properties props = new Properties();
            for (Property property : configuration) {
                props.setProperty(property.getName(), property.getValue());
            }
            return props;
        }

        public void setConfiguration(final Properties configuration) {
            Set<Property> conf = new HashSet<>();

            for (Map.Entry<Object, Object> objectEntry : configuration.entrySet()) {
                conf.add(new Property((String) objectEntry.getKey(), (String) objectEntry.getValue()));
            }

            this.configuration = conf;
        }

        public void addConfigurationProperty(final String name, final String value) {
            this.configuration.add(new Property(name, value));
        }

        @XmlRootElement(name = PROPERTY)
        @XmlAccessorType(XmlAccessType.FIELD)
        public static class Property {
            @XmlAttribute(name = ATTR_NAME)
            private String name;
            @XmlAttribute(name = ATTR_VALUE)
            private String value;

            public Property() {
            }

            public Property(String name, String value) {
                this.name = name;
                this.value = value;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }
        }
    }

    @XmlRootElement(name = ATTRIBUTE)
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Attribute {
        @XmlAttribute(name = ATTR_NAME)
        private String name;

        public Attribute() {
        }

        public Attribute(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    @XmlAttribute(name = ATTR_ENTITY_ID)
    private String entityID;
    @XmlAttribute(name = ATTR_SSL_POLICY)
    private String sslPolicy;
    @XmlAttribute(name = ATTR_FORCE_AUTHENTICATION)
    private boolean forceAuthentication;
    @XmlAttribute(name = ATTR_IS_PASSIVE)
    private boolean isPassive;
    @XmlAttribute(name = ATTR_TURN_OFF_CHANGE_SESSSION_ID_ON_LOGIN)
    private boolean turnOffChangeSessionIdOnLogin;
    @XmlAttribute(name = ATTR_LOGOUT_PAGE)
    private String logoutPage;

    @XmlElementWrapper(name = KEYS)
    @XmlElement(name = KEY)
    private List<Key> keys;
    @XmlAttribute(name = ATTR_NAME_ID_POLICY_FORMAT)
    private String nameIDPolicyFormat;
    @XmlElement(name = PRINCIPAL_NAME_MAPPING)
    private PrincipalNameMapping principalNameMapping;

    @XmlElementWrapper(name = ROLE_IDENTIFIERS)
    @XmlElement(name = ATTRIBUTE)
    private Set<Attribute> roleAttributes;

    @XmlElement(name = ROLE_MAPPINGS_PROVIDER)
    private RoleMappingsProviderConfig roleMappingsProviderConfig;
    @XmlElement(name = KeycloakSamlAdapterNames.IDP)
    private IDP idp;
    @XmlAttribute(name = ATTR_AUTODETECT_BEARER_ONLY)
    private boolean autodetectBearerOnly;
    @XmlAttribute(name = ATTR_KEEP_DOM_ASSERTION)
    private boolean keepDOMAssertion;

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(String entityID) {
        this.entityID = entityID;
    }

    public String getSslPolicy() {
        return sslPolicy;
    }

    public void setSslPolicy(String sslPolicy) {
        this.sslPolicy = sslPolicy;
    }

    public boolean isForceAuthentication() {
        return forceAuthentication;
    }

    public void setForceAuthentication(Boolean forceAuthentication) {
        this.forceAuthentication = forceAuthentication != null && forceAuthentication;
    }

    public boolean isIsPassive() {
        return isPassive;
    }

    public void setIsPassive(Boolean isPassive) {
        this.isPassive = isPassive != null && isPassive;
    }

    public boolean isTurnOffChangeSessionIdOnLogin() {
        return turnOffChangeSessionIdOnLogin;
    }

    public void setTurnOffChangeSessionIdOnLogin(Boolean turnOffChangeSessionIdOnLogin) {
        this.turnOffChangeSessionIdOnLogin = turnOffChangeSessionIdOnLogin != null && turnOffChangeSessionIdOnLogin;
    }

    public boolean isKeepDOMAssertion() {
        return keepDOMAssertion;
    }

    public void setKeepDOMAssertion(Boolean keepDOMAssertion) {
        this.keepDOMAssertion = keepDOMAssertion != null && keepDOMAssertion;
    }

    public List<Key> getKeys() {
        return keys;
    }

    public void setKeys(List<Key> keys) {
        this.keys = keys;
    }

    public String getNameIDPolicyFormat() {
        return nameIDPolicyFormat;
    }

    public void setNameIDPolicyFormat(String nameIDPolicyFormat) {
        this.nameIDPolicyFormat = nameIDPolicyFormat;
    }

    public PrincipalNameMapping getPrincipalNameMapping() {
        return principalNameMapping;
    }

    public void setPrincipalNameMapping(PrincipalNameMapping principalNameMapping) {
        this.principalNameMapping = principalNameMapping;
    }

    public Set<String> getRoleAttributes() {
        Set<String> roles = new HashSet<>();
        for (Attribute roleAttribute : roleAttributes) {
            roles.add(roleAttribute.getName());
        }
        return roles;
    }

    public void setRoleAttributes(Set<String> roleAttributes) {
        Set<Attribute> attributes = new HashSet<>();
        for (String roleAttribute : roleAttributes) {
            attributes.add(new Attribute(roleAttribute));
        }

        this.roleAttributes = attributes;
    }

    public RoleMappingsProviderConfig getRoleMappingsProviderConfig() {
        return this.roleMappingsProviderConfig;
    }

    public void setRoleMappingsProviderConfig(final RoleMappingsProviderConfig provider) {
        this.roleMappingsProviderConfig = provider;
    }

    public IDP getIdp() {
        return idp;
    }

    public void setIdp(IDP idp) {
        this.idp = idp;
    }

    public String getLogoutPage() {
        return logoutPage;
    }

    public void setLogoutPage(String logoutPage) {
        this.logoutPage = logoutPage;
    }

    public boolean isAutodetectBearerOnly() {
        return autodetectBearerOnly;
    }

    public void setAutodetectBearerOnly(Boolean autodetectBearerOnly) {
        this.autodetectBearerOnly = autodetectBearerOnly != null && autodetectBearerOnly;
    }
}
