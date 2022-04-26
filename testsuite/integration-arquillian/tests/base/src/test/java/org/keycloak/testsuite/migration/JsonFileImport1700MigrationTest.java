package org.keycloak.testsuite.migration;

import org.junit.Test;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@AuthServerContainerExclude(value = {AuthServerContainerExclude.AuthServer.REMOTE})
public class JsonFileImport1700MigrationTest extends AbstractJsonFileImportMigrationTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        Map<String, RealmRepresentation> reps = null;
        try {
            reps = ImportUtils.getRealmsFromStream(JsonSerialization.mapper, IOUtil.class.getResourceAsStream("/migration-test/migration-realm-17.0.0.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (RealmRepresentation rep : reps.values()) {
            testRealms.add(rep);
        }
    }

    @Test
    public void migration17() throws Exception {
    }
}