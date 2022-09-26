package org.keycloak.testsuite.cluster;

import org.junit.jupiter.api.BeforeEach;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.ContainerInfo;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractInvalidationClusterTestWithTestRealm<T, TR> extends AbstractInvalidationClusterTest<T, TR> {

    protected String testRealmName = null;
    
    @BeforeEach
    public void createTestRealm() {
        createTestRealm(frontendNode());
    }
    
    protected void createTestRealm(ContainerInfo node) {
        RealmRepresentation r = createTestRealmRepresentation();
        getAdminClientFor(node).realms().create(r);
        testRealmName = r.getRealm();
    }
    
}
