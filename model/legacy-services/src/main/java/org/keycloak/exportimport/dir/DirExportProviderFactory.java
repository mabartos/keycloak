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

package org.keycloak.exportimport.dir;

import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportProvider;
import org.keycloak.exportimport.ExportProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DirExportProviderFactory implements ExportProviderFactory {

    public static final String PROVIDER_ID = "dir";
    public static final String DIR = "dir";
    public static final String REALM_ID = "realmId";
    private Config.Scope config;

    @Override
    public ExportProvider create(KeycloakSession session) {
        String dir = config.get(DIR, System.getProperty(ExportImportConfig.DIR));
        String realmId = config.get(REALM_ID, System.getProperty(ExportImportConfig.REALM_NAME));
        return new DirExportProvider(session.getKeycloakSessionFactory()).withDir(dir).withRealmId(realmId);
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(REALM_ID)
                .type("string")
                .helpText("Realm to export")
                .add()

                .property()
                .name(DIR)
                .type("string")
                .helpText("Directory to export to")
                .add()

                .build();
    }

}
