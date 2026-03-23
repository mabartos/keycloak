/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.utils.KeycloakSessionUtil;

public class AdminClientUpdateContext extends AbstractAdminClientCRUDContext {
    @Deprecated
    private ClientRepresentation proposedClientRepresentation;
    private final ClientModel proposedClient;
    private final ClientModel targetClient;

    public AdminClientUpdateContext(ClientModel proposedClient, ClientModel targetClient, AdminAuth adminAuth) {
        super(adminAuth);
        this.proposedClient = proposedClient;
        this.targetClient = targetClient;
    }

    @Deprecated
    public AdminClientUpdateContext(ClientRepresentation proposedClientRepresentation, ClientModel targetClient, AdminAuth adminAuth) {
        super(adminAuth);
        this.proposedClientRepresentation = proposedClientRepresentation;
        this.proposedClient = null;
        this.targetClient = targetClient;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.UPDATE;
    }

    @Override
    @Deprecated
    public ClientRepresentation getProposedClientRepresentation() {
        return ModelToRepresentation.toRepresentation(proposedClient, KeycloakSessionUtil.getKeycloakSession());
    }

    @Override
    public ClientModel getProposedClient() {
        return proposedClient;
    }

    @Override
    public ClientModel getTargetClient() {
        return targetClient;
    }
}
