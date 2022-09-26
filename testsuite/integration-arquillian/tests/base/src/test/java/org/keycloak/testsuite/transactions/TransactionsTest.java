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

package org.keycloak.testsuite.transactions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TransactionsTest extends AbstractKeycloakTest {

    @Test
    public void testTransactionActive() {
        testingClient.server().run(
                session -> {
                    Assertions.assertTrue(session.getTransactionManager().isActive());
                    session.getTransactionManager().commit();
                    Assertions.assertFalse(session.getTransactionManager().isActive());

                    session.getTransactionManager().begin();
                    Assertions.assertTrue(session.getTransactionManager().isActive());
                    session.getTransactionManager().rollback();
                    Assertions.assertFalse(session.getTransactionManager().isActive());
                }
        );
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

}
