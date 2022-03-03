package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Map;

public class ConditionalTestAuthenticator implements ConditionalAuthenticator {

    public static final ConditionalTestAuthenticator SINGLETON = new ConditionalTestAuthenticator();

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        Map<String, String> config = context.getAuthenticatorConfig().getConfig();
        String param1 = config.get(TestConditionalFactory.PARAM1);
        String param2 = config.get(TestConditionalFactory.PARAM2);

        System.err.println("MATCH CONDITION");
        System.err.println(param1);
        System.err.println(param2);

        return true;
    }
}
