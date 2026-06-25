package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

/**
 * Lightweight context for read-only policy condition evaluation against a specific client.
 * Used to check if a policy applies to a client without triggering executors.
 */
public class ClientPolicyCheckContext implements ClientCRUDClientAvailableContext {

    private final ClientModel client;

    public ClientPolicyCheckContext(ClientModel client) {
        this.client = client;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.UPDATED;
    }

    @Override
    public ClientModel getTargetClient() {
        return client;
    }
}
