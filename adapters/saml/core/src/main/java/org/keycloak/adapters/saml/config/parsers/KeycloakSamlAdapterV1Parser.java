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

package org.keycloak.adapters.saml.config.parsers;

import org.keycloak.adapters.saml.config.KeycloakSamlAdapter;
import org.keycloak.adapters.saml.config.KeycloakSamlAdapterNames;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import java.util.Iterator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakSamlAdapterV1Parser extends AbstractKeycloakSamlAdapterV1Parser<KeycloakSamlAdapter> {

    private static final KeycloakSamlAdapterV1Parser INSTANCE = new KeycloakSamlAdapterV1Parser();

    private KeycloakSamlAdapterV1Parser() {
        super(KeycloakSamlAdapterV1QNames.KEYCLOAK_SAML_ADAPTER);
    }

    public static KeycloakSamlAdapterV1Parser getInstance() {
        return INSTANCE;
    }

    @Override
    protected KeycloakSamlAdapter instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        KeycloakSamlAdapter adapter = new KeycloakSamlAdapter();

        final String xmlns = getNamespaceURI(element, KeycloakSamlAdapterNames.XMLNS);
        adapter.setXmlns(xmlns == null ? KeycloakSamlAdapterV1QNames.NS_URI : xmlns);

        final String xmlnsXsi = element.getNamespaceURI(KeycloakSamlAdapterNames.XSI);
        adapter.setXmlnsXsi(xmlnsXsi == null ? KeycloakSamlAdapterV1QNames.XMLNS_XSI_DEFAULT : xmlnsXsi);

        final QName schemaLocationQName = new QName(xmlnsXsi, KeycloakSamlAdapterNames.SCHEMA_LOCATION, KeycloakSamlAdapterNames.XSI);
        final String schemaLocation = StaxParserUtil.getAttributeValue(element, schemaLocationQName);
        adapter.setSchemaLocation(schemaLocation == null ? KeycloakSamlAdapterV1QNames.NS_URI : schemaLocation);

        return adapter;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, KeycloakSamlAdapter target, KeycloakSamlAdapterV1QNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case SP:
                target.addSp(SpParser.getInstance().parse(xmlEventReader));
                break;

            default:
                // Ignore unknown tags
                StaxParserUtil.bypassElementBlock(xmlEventReader);
        }
    }

    private static String getNamespaceURI(StartElement element, String prefix) {
        Iterator iterator = element.getNamespaces();
        while (iterator.hasNext()) {
            Namespace namespace = (Namespace) iterator.next();
            if (namespace.getName().getPrefix().equals(prefix)) {
                return namespace.getNamespaceURI();
            }
        }
        return null;
    }
}
