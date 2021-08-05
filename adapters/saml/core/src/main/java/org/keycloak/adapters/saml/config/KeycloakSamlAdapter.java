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
import java.util.LinkedList;
import java.util.List;

import static org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames.KEYCLOAK_SAML_ADAPTER;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@XmlRootElement(name = KEYCLOAK_SAML_ADAPTER)
@XmlAccessorType(XmlAccessType.FIELD)
public class KeycloakSamlAdapter implements Serializable {

    @XmlAttribute(name = KeycloakSamlAdapterNames.XMLNS)
    private String xmlns;

    @XmlAttribute(name = KeycloakSamlAdapterNames.XMLNS_XSI)
    private String xmlnsXsi;

    @XmlAttribute(name = KeycloakSamlAdapterNames.XSI_SCHEMA_LOCATION)
    private String schemaLocation;

    @XmlElement(name = KeycloakSamlAdapterNames.SP)
    private final List<SP> sps = new LinkedList<>();

    public List<SP> getSps() {
        return sps;
    }

    public void addSp(SP sp) {
        sps.add(sp);
    }

    public String getXmlns() {
        return xmlns;
    }

    public void setXmlns(String xmlns) {
        this.xmlns = xmlns;
    }

    public String getXmlnsXsi() {
        return xmlnsXsi;
    }

    public void setXmlnsXsi(String xmlnsXsi) {
        this.xmlnsXsi = xmlnsXsi;
    }

    public String getSchemaLocation() {
        return schemaLocation;
    }

    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
    }
}
