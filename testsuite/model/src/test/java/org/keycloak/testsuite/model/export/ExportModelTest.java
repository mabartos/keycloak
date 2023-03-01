/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model.export;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.exportimport.ExportProvider;
import org.keycloak.exportimport.dir.DirExportProviderFactory;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@RequireProvider(value = ExportProvider.class)
public class ExportModelTest extends KeycloakModelTest {

    public static final String REALM_NAME = "realm";
    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        // initialize a minimal realm with necessary entries to avoid any NPEs
        RealmModel realm = createRealm(s, REALM_NAME);
        realm.setSslRequired(SslRequired.NONE);
        RoleModel role = s.roles().addRealmRole(realm, "default");
        realm.setDefaultRole(role);
        this.realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(realmId);
    }

    @Test
    @RequireProvider(value = ExportProvider.class, only = SingleFileExportProviderFactory.PROVIDER_ID)
    public void testExportSingleFile() throws IOException {
        Path singleFileExport = null;
        try {
            // create a temporary file, then delete it to later test that it has been re-created by the export
            singleFileExport = Files.createTempFile("singleFileExport", ".json");
            Files.delete(singleFileExport);

            CONFIG.spi("export")
                    .config("exporter", SingleFileExportProviderFactory.PROVIDER_ID);
            CONFIG.spi("export")
                    .provider(SingleFileExportProviderFactory.PROVIDER_ID)
                    .config(SingleFileExportProviderFactory.FILE, singleFileExport.toAbsolutePath().toString());
            CONFIG.spi("export")
                    .provider(SingleFileExportProviderFactory.PROVIDER_ID)
                    .config(SingleFileExportProviderFactory.REALM_NAME, REALM_NAME);

            withRealm(realmId, (session, realm) -> {
                ExportImportConfig.setAction(ExportImportConfig.ACTION_EXPORT);
                ExportImportManager exportImportManager = new ExportImportManager(session);
                exportImportManager.runExport();
                return null;
            });

            // file will exist if export was successful
            Assert.assertTrue(Files.exists(singleFileExport));
        } finally {
            CONFIG.spi("export")
                    .config("exporter", null);
            CONFIG.spi("export")
                    .provider(SingleFileExportProviderFactory.PROVIDER_ID)
                    .config(SingleFileExportProviderFactory.FILE, null);
            CONFIG.spi("export")
                    .provider(SingleFileExportProviderFactory.PROVIDER_ID)
                    .config(SingleFileExportProviderFactory.REALM_NAME, null);
            if (singleFileExport != null && Files.exists(singleFileExport)) {
                Files.delete(singleFileExport);
            }
        }
    }

    @Test
    @RequireProvider(value = ExportProvider.class, only = DirExportProviderFactory.PROVIDER_ID)
    public void testExportDirectory() throws IOException {
        Path dirExport = null;
        try {
            // create a temporary folder, then delete it to later test that it has been re-created by the export
            dirExport = Files.createTempDirectory("dirFileExport");
            Files.delete(dirExport);

            CONFIG.spi("export")
                    .config("exporter", DirExportProviderFactory.PROVIDER_ID);
            CONFIG.spi("export")
                    .provider(DirExportProviderFactory.PROVIDER_ID)
                    .config(DirExportProviderFactory.DIR, dirExport.toAbsolutePath().toString());
            CONFIG.spi("export")
                    .provider(DirExportProviderFactory.PROVIDER_ID)
                    .config(DirExportProviderFactory.REALM_NAME, REALM_NAME);

            withRealm(realmId, (session, realm) -> {
                ExportImportConfig.setAction(ExportImportConfig.ACTION_EXPORT);
                ExportImportManager exportImportManager = new ExportImportManager(session);
                exportImportManager.runExport();
                return null;
            });

            // file will exist if export was successful
            Assert.assertTrue(Files.exists(dirExport.resolve(REALM_NAME + "-realm.json")));
        } finally {
            CONFIG.spi("export")
                    .config("exporter", null);
            CONFIG.spi("export")
                    .provider(DirExportProviderFactory.PROVIDER_ID)
                    .config(DirExportProviderFactory.DIR, null);
            CONFIG.spi("export")
                    .provider(DirExportProviderFactory.PROVIDER_ID)
                    .config(DirExportProviderFactory.REALM_NAME, null);
            if (dirExport != null && Files.exists(dirExport)) {
                try (Stream<Path> walk = Files.list(dirExport)) {
                    walk.forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
                Files.delete(dirExport);
            }
        }
    }

}
