package org.keycloak.tests.oauth.tokenexchange;

import java.util.Map;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenResponseContext;
import org.keycloak.services.clientpolicy.executor.RejectMayActClaimExecutor;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.Assertions;

final class DelegationPolicyAssertions {

    private DelegationPolicyAssertions() {
    }

    static void assertPolicy(RunOnServerClient server, AccessToken login, String actorId, boolean missingPermission) {
        String sessionId = login.getSessionId();
        String clientId = login.getIssuedFor();
        for (boolean skipCheck : new boolean[] { false, true }) {
            String outcome = server.fetchString(session -> {
                RealmModel realm = session.getContext().getRealm();
                ClientModel client = realm.getClientByClientId(clientId);
                UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);
                var clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
                var ctx = DefaultClientSessionContext.fromClientSessionScopeParameter(clientSession, session);

                // Exercise the executor with an in-memory response that has no delegation consent.
                AccessToken token = new AccessToken();
                token.getOtherClaims().put(IDToken.MAY_ACT, Map.of("sub", actorId));
                var response = new TokenManager().responseBuilder(realm, client, null, session, userSession, ctx).accessToken(token);
                var executor = new RejectMayActClaimExecutor(session);
                executor.setupConfiguration(new RejectMayActClaimExecutor.Configuration(false,
                        missingPermission && skipCheck, missingPermission || skipCheck));
                try {
                    executor.executeOnEvent(new TokenResponseContext(null, null, ctx, response));
                    return "allowed";
                } catch (ClientPolicyException e) {
                    return e.getErrorDetail();
                }
            });
            Assertions.assertEquals(skipCheck ? "allowed" : "Invalid may_act sub in the token", outcome);
        }
    }
}
