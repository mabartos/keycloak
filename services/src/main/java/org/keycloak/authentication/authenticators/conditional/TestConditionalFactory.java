package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.Config;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class TestConditionalFactory implements ConditionalAuthenticatorFactory {
    public static final String PROVIDER_ID = "test-conditional-provider";

    public static final String PARAM1 = "param1";
    public static final String PARAM2 = "param2";

    @Override
    public String getDisplayType() {
        return "Condition - TEST";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[0];
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Test conditional";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {

        List<ProviderConfigProperty> list = new ArrayList<>();

        ProviderConfigProperty authNoteName = new ProviderConfigProperty();
        authNoteName.setType(ProviderConfigProperty.STRING_TYPE);
        authNoteName.setName(PARAM1);
        authNoteName.setLabel("Attribute name");
        authNoteName.setHelpText("Name of the attribute to check");
        authNoteName.setDefaultValue("here");

        ProviderConfigProperty authNoteExpectedValue = new ProviderConfigProperty();
        authNoteExpectedValue.setType(ProviderConfigProperty.STRING_TYPE);
        authNoteExpectedValue.setName(PARAM2);
        authNoteExpectedValue.setLabel("Expected attribute value");
        authNoteExpectedValue.setHelpText("Expected value in the attribute");
        authNoteExpectedValue.setDefaultValue("here2");

        list.add(authNoteName);
        list.add(authNoteExpectedValue);
        return list;
    }

    @Override
    public void init(Config.Scope config) {

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
    public ConditionalAuthenticator getSingleton() {
        return ConditionalTestAuthenticator.SINGLETON;
    }
}
