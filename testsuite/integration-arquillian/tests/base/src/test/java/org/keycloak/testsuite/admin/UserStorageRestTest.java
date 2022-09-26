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

package org.keycloak.testsuite.admin;

import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ComponentTypeRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.CommonLDAPGroupMapperConfig;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.storage.ldap.mappers.membership.role.RoleLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.role.RoleMapperConfig;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.authentication.AbstractAuthenticationTest;
import org.keycloak.testsuite.util.AdminEventPaths;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserStorageRestTest extends AbstractAdminTest {
    private AuthenticationExecutionInfoRepresentation findKerberosExecution() {
        AuthenticationExecutionInfoRepresentation kerberosExecution = null;
        List<AuthenticationExecutionInfoRepresentation> executionReps = realm.flows().getExecutions("browser");
        kerberosExecution = AbstractAuthenticationTest.findExecutionByProvider("auth-spnego", executionReps);

        Assertions.assertNotNull(kerberosExecution);
        return kerberosExecution;
    }

    private String createComponent(ComponentRepresentation rep) {
        Response resp = realm.components().add(rep);
        Assertions.assertEquals(201, resp.getStatus());
        resp.close();
        String id = ApiUtil.getCreatedId(resp);

        assertAdminEvents.clear();
        return id;
    }

    private void removeComponent(String id) {
        realm.components().component(id).remove();
        assertAdminEvents.clear();
    }

    private void assertFederationProvider(ComponentRepresentation rep, String id, String displayName, String providerId,
                                          String... config) {
        Assertions.assertEquals(id, rep.getId());
        Assertions.assertEquals(displayName, rep.getName());
        Assertions.assertEquals(providerId, rep.getProviderId());

        Assertions.assertMultivaluedMap(rep.getConfig(), config);
    }


    @Test
    public void testKerberosAuthenticatorEnabledAutomatically() {
        // Assert kerberos authenticator DISABLED
        AuthenticationExecutionInfoRepresentation kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.DISABLED.toString());

        // create LDAP provider with kerberos
        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION, "true");

        String id = createComponent(ldapRep);

        // Assert kerberos authenticator ALTERNATIVE
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString());

        // Switch kerberos authenticator to DISABLED
        kerberosExecution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED.toString());
        realm.flows().updateExecutions("browser", kerberosExecution);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("browser"), kerberosExecution, ResourceType.AUTH_EXECUTION);

        // update LDAP provider with kerberos (without changing kerberos switch)
        ldapRep = realm.components().component(id).toRepresentation();
        realm.components().component(id).update(ldapRep);
        assertAdminEvents.clear();

        // Assert kerberos authenticator is still DISABLED
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.DISABLED.toString());

        // update LDAP provider with kerberos (with changing kerberos switch to disabled)
        ldapRep = realm.components().component(id).toRepresentation();
        ldapRep.getConfig().putSingle(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION, "false");
        realm.components().component(id).update(ldapRep);
        assertAdminEvents.clear();

        // Assert kerberos authenticator is still DISABLED
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.DISABLED.toString());

        // update LDAP provider with kerberos (with changing kerberos switch to enabled)
        ldapRep = realm.components().component(id).toRepresentation();
        ldapRep.getConfig().putSingle(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION, "true");
        realm.components().component(id).update(ldapRep);
        assertAdminEvents.clear();

        // Assert kerberos authenticator is still ALTERNATIVE
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString());

        // Cleanup
        kerberosExecution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED.toString());
        realm.flows().updateExecutions("browser", kerberosExecution);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("browser"), kerberosExecution, ResourceType.AUTH_EXECUTION);
        removeComponent(id);
    }

    @Test
    public void testKerberosAuthenticatorChangedOnlyIfDisabled() {
        // Change kerberos to REQUIRED
        AuthenticationExecutionInfoRepresentation kerberosExecution = findKerberosExecution();
        kerberosExecution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString());
        realm.flows().updateExecutions("browser", kerberosExecution);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("browser"), kerberosExecution, ResourceType.AUTH_EXECUTION);

        // create LDAP provider with kerberos
        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION, "true");

        String id = createComponent(ldapRep);


        // Assert kerberos authenticator still REQUIRED
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.REQUIRED.toString());

        // update LDAP provider with kerberos
        ldapRep = realm.components().component(id).toRepresentation();
        realm.components().component(id).update(ldapRep);
        assertAdminEvents.clear();

        // Assert kerberos authenticator still REQUIRED
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.REQUIRED.toString());

        // Cleanup
        kerberosExecution.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED.toString());
        realm.flows().updateExecutions("browser", kerberosExecution);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.authUpdateExecutionPath("browser"), kerberosExecution, ResourceType.AUTH_EXECUTION);
        removeComponent(id);

    }


    // KEYCLOAK-4438
    @Test
    public void testKerberosAuthenticatorDisabledWhenProviderRemoved() {
        // Assert kerberos authenticator DISABLED
        AuthenticationExecutionInfoRepresentation kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.DISABLED.toString());

        // create LDAP provider with kerberos
        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION, "true");


        String id = createComponent(ldapRep);

        // Assert kerberos authenticator ALTERNATIVE
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString());

        // Remove LDAP provider
        realm.components().component(id).remove();

        // Assert kerberos authenticator DISABLED
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.DISABLED.toString());

        // Add kerberos provider
        ComponentRepresentation kerberosRep = new ComponentRepresentation();
        kerberosRep.setName("kerberos");
        kerberosRep.setProviderId("kerberos");
        kerberosRep.setProviderType(UserStorageProvider.class.getName());
        kerberosRep.setConfig(new MultivaluedHashMap<>());
        kerberosRep.getConfig().putSingle("priority", Integer.toString(2));

        id = createComponent(kerberosRep);


        // Assert kerberos authenticator ALTERNATIVE
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.ALTERNATIVE.toString());

        // Switch kerberos authenticator to REQUIRED
        kerberosExecution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString());
        realm.flows().updateExecutions("browser", kerberosExecution);

        // Remove Kerberos provider
        realm.components().component(id).remove();

        // Assert kerberos authenticator DISABLED
        kerberosExecution = findKerberosExecution();
        Assertions.assertEquals(kerberosExecution.getRequirement(), AuthenticationExecutionModel.Requirement.DISABLED.toString());
    }


    @Test
    public void testValidateAndCreateLdapProviderCustomSearchFilter() {
        // Invalid filter

        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "dc=something");

        Response resp = realm.components().add(ldapRep);
        Assertions.assertEquals(400, resp.getStatus());
        resp.close();

        // Invalid filter
        ldapRep.getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something");
        resp = realm.components().add(ldapRep);
        Assertions.assertEquals(400, resp.getStatus());
        resp.close();

        // Invalid filter
        ldapRep.getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "dc=something)");
        resp = realm.components().add(ldapRep);
        Assertions.assertEquals(400, resp.getStatus());
        resp.close();

        // Assert nothing created so far
        Assertions.assertTrue(realm.components().query(realmId, UserStorageProvider.class.getName()).isEmpty());
        assertAdminEvents.assertEmpty();


        // Valid filter. Creation success
        ldapRep.getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something)");
        String id1 = createComponent(ldapRep);

        // Missing filter is ok too. Creation success
        ComponentRepresentation ldapRep2 = new ComponentRepresentation();
        ldapRep2.setName("ldap3");
        ldapRep2.setProviderId("ldap");
        ldapRep2.setProviderType(UserStorageProvider.class.getName());
        ldapRep2.setConfig(new MultivaluedHashMap<>());
        ldapRep2.getConfig().putSingle("priority", Integer.toString(2));
        ldapRep2.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.UNSYNCED.name());
        ldapRep2.getConfig().putSingle(LDAPConstants.BIND_DN, "cn=manager");
        ldapRep2.getConfig().putSingle(LDAPConstants.BIND_CREDENTIAL, "password");
        String id2 = createComponent(ldapRep2);

        // Assert both providers created
        List<ComponentRepresentation> providerInstances = realm.components().query(realmId, UserStorageProvider.class.getName());
        Assertions.assertEquals(providerInstances.size(), 2);

        // Cleanup
        removeComponent(id1);
        removeComponent(id2);
    }

    @Test
    public void testValidateAndCreateLdapProviderEditMode() {
        // Test provider without editMode should fail
        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().remove(LDAPConstants.EDIT_MODE);

        Response resp = realm.components().add(ldapRep);
        Assertions.assertEquals(400, resp.getStatus());
        resp.close();

        // Test provider with READ_ONLY edit mode and validatePasswordPolicy will fail
        ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.READ_ONLY.name());
        ldapRep.getConfig().putSingle(LDAPConstants.VALIDATE_PASSWORD_POLICY, "true");
        resp = realm.components().add(ldapRep);
        Assertions.assertEquals(400, resp.getStatus());
        resp.close();

        // Test provider with UNSYNCED edit mode and validatePasswordPolicy will fail
        ldapRep.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.UNSYNCED.name());
        ldapRep.getConfig().putSingle(LDAPConstants.VALIDATE_PASSWORD_POLICY, "true");
        resp = realm.components().add(ldapRep);
        Assertions.assertEquals(400, resp.getStatus());
        resp.close();

        // Test provider with WRITABLE edit mode and validatePasswordPolicy will fail
        ldapRep.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.name());
        ldapRep.getConfig().putSingle(LDAPConstants.SYNC_REGISTRATIONS, "true");
        String id1 = createComponent(ldapRep);

        // Cleanup
        removeComponent(id1);
    }

    @Test
    public void testUpdateProvider() {
        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(LDAPConstants.BIND_DN, "cn=manager");
        ldapRep.getConfig().putSingle(LDAPConstants.BIND_CREDENTIAL, "password");
        String id = createComponent(ldapRep);

        // Assert update with invalid filter should fail
        ldapRep = realm.components().component(id).toRepresentation();
        ldapRep.getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something2");
        ldapRep.getConfig().putSingle(LDAPConstants.BIND_DN, "cn=manager-updated");
        try {
            realm.components().component(id).update(ldapRep);
            Assertions.fail("Not expected to successfull update");
        } catch (BadRequestException bre) {
            // Expected
        }

        // Assert nothing was updated
        assertFederationProvider(realm.components().component(id).toRepresentation(), id, "ldap2", "ldap", LDAPConstants.BIND_DN, "cn=manager", LDAPConstants.BIND_CREDENTIAL, "**********");

        // Change filter to be valid
        ldapRep.getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something2)");
        realm.components().component(id).update(ldapRep);
        assertAdminEvents.clear();

        // Assert updated successfully
        ldapRep = realm.components().component(id).toRepresentation();
        assertFederationProvider(ldapRep, id, "ldap2", "ldap", LDAPConstants.BIND_DN, "cn=manager-updated", LDAPConstants.BIND_CREDENTIAL, "**********",
                LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something2)");

        // Assert update displayName
        ldapRep.setName("ldap2");
        realm.components().component(id).update(ldapRep);

        assertFederationProvider(realm.components().component(id).toRepresentation(), id, "ldap2", "ldap",LDAPConstants.BIND_DN, "cn=manager-updated", LDAPConstants.BIND_CREDENTIAL, "**********",
                LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something2)");



        // Cleanup
        removeComponent(id);
    }


    // KEYCLOAK-12934
    @Test
    public void testLDAPMapperProviderConfigurationForVendorOther() {
        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(LDAPConstants.VENDOR, LDAPConstants.VENDOR_OTHER);
        String ldapModelId = createComponent(ldapRep);

        ComponentTypeRepresentation groupLDAPMapperType = findMapperTypeConfiguration(ldapModelId, GroupLDAPStorageMapperFactory.PROVIDER_ID);
        ConfigPropertyRepresentation groupRetrieverConfigProperty = getUserRolesRetrieveStrategyConfigProperty(groupLDAPMapperType, CommonLDAPGroupMapperConfig.USER_ROLES_RETRIEVE_STRATEGY);

        // LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY is expected to be present just for the active directory
        List<String> options = groupRetrieverConfigProperty.getOptions();
        Assertions.assertNames(options, GroupMapperConfig.LOAD_GROUPS_BY_MEMBER_ATTRIBUTE, GroupMapperConfig.GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE);
        Assertions.assertFalse(groupRetrieverConfigProperty.getHelpText().contains("LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY"));

        ComponentTypeRepresentation roleLDAPMapperType = findMapperTypeConfiguration(ldapModelId, RoleLDAPStorageMapperFactory.PROVIDER_ID);
        ConfigPropertyRepresentation roleRetrieverConfigProperty = getUserRolesRetrieveStrategyConfigProperty(roleLDAPMapperType, CommonLDAPGroupMapperConfig.USER_ROLES_RETRIEVE_STRATEGY);

        // LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY is expected to be present just for the active directory
        options = roleRetrieverConfigProperty.getOptions();
        Assertions.assertNames(options, RoleMapperConfig.LOAD_ROLES_BY_MEMBER_ATTRIBUTE, RoleMapperConfig.GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE);
        Assertions.assertFalse(roleRetrieverConfigProperty.getHelpText().contains("LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY"));

        // Cleanup including mappers
        removeComponent(ldapModelId);
    }

    // KEYCLOAK-12934
    @Test
    public void testLDAPMapperProviderConfigurationForVendorMSAD() {
        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(LDAPConstants.VENDOR, LDAPConstants.VENDOR_ACTIVE_DIRECTORY);
        String ldapModelId = createComponent(ldapRep);

        ComponentTypeRepresentation groupLDAPMapperType = findMapperTypeConfiguration(ldapModelId, GroupLDAPStorageMapperFactory.PROVIDER_ID);
        ConfigPropertyRepresentation groupRetrieverConfigProperty = getUserRolesRetrieveStrategyConfigProperty(groupLDAPMapperType, CommonLDAPGroupMapperConfig.USER_ROLES_RETRIEVE_STRATEGY);

        // LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY is expected to be present just for the active directory
        List<String> options = groupRetrieverConfigProperty.getOptions();
        Assertions.assertNames(options, GroupMapperConfig.LOAD_GROUPS_BY_MEMBER_ATTRIBUTE, GroupMapperConfig.GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE,
                GroupMapperConfig.LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY);
        Assertions.assertTrue(groupRetrieverConfigProperty.getHelpText().contains("LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY"));

        ComponentTypeRepresentation roleLDAPMapperType = findMapperTypeConfiguration(ldapModelId, RoleLDAPStorageMapperFactory.PROVIDER_ID);
        ConfigPropertyRepresentation roleRetrieverConfigProperty = getUserRolesRetrieveStrategyConfigProperty(roleLDAPMapperType, CommonLDAPGroupMapperConfig.USER_ROLES_RETRIEVE_STRATEGY);

        // LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY is expected to be present just for the active directory
        options = roleRetrieverConfigProperty.getOptions();
        Assertions.assertNames(options, RoleMapperConfig.LOAD_ROLES_BY_MEMBER_ATTRIBUTE, RoleMapperConfig.GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE,
                RoleMapperConfig.LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY);
        Assertions.assertTrue(roleRetrieverConfigProperty.getHelpText().contains("LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY"));

        // Cleanup including mappers
        removeComponent(ldapModelId);
    }

    private ComponentRepresentation createBasicLDAPProviderRep() {
        ComponentRepresentation ldapRep = new ComponentRepresentation();
        ldapRep.setName("ldap2");
        ldapRep.setProviderId("ldap");
        ldapRep.setProviderType(UserStorageProvider.class.getName());
        ldapRep.setConfig(new MultivaluedHashMap<>());
        ldapRep.getConfig().putSingle("priority", Integer.toString(2));
        ldapRep.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.name());
        return ldapRep;
    }

    private ComponentTypeRepresentation findMapperTypeConfiguration(String ldapModelId, String mapperProviderId) {
        ComponentResource ldapProvider = realm.components().component(ldapModelId);
        List<ComponentTypeRepresentation> componentTypes = ldapProvider.getSubcomponentConfig(LDAPStorageMapper.class.getName());

        return componentTypes.stream()
                .filter(componentType -> mapperProviderId.equals(componentType.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Not able to find mapper with provider id: " + mapperProviderId));
    }

    private  ConfigPropertyRepresentation getUserRolesRetrieveStrategyConfigProperty(ComponentTypeRepresentation componentType, String propertyName) {
        return componentType.getProperties().stream()
                .filter(configPropertyRep -> propertyName.equals(configPropertyRep.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Not able to find config property with name: " + propertyName));
    }

/*
    @Test
    public void testProviderFactories() {
        List<UserFederationProviderFactoryRepresentation> providerFactories = userFederation().getProviderFactories();
        Assertions.assertNames(providerFactories, "ldap", "kerberos", "dummy", "dummy-configurable");

        // Builtin provider without properties
        UserFederationProviderFactoryRepresentation ldapProvider = userFederation().getProviderFactory("ldap");
        Assertions.assertEquals(ldapProvider.getId(), "ldap");
        Assertions.assertEquals(0, ldapProvider.getOptions().size());

        // Configurable through the "old-way" options
        UserFederationProviderFactoryRepresentation dummyProvider = userFederation().getProviderFactory("dummy");
        Assertions.assertEquals(dummyProvider.getId(), "dummy");
        Assertions.assertNames(new LinkedList<>(dummyProvider.getOptions()), "important.config");

        // Configurable through the "new-way" ConfiguredProvider
        UserFederationProviderFactoryRepresentation dummyConfiguredProvider = userFederation().getProviderFactory("dummy-configurable");
        Assertions.assertEquals(dummyConfiguredProvider.getId(), "dummy-configurable");
        Assertions.assertTrue(dummyConfiguredProvider.getOptions() == null || dummyConfiguredProvider.getOptions().isEmpty());
        Assertions.assertEquals("Dummy User Federation Provider Help Text", dummyConfiguredProvider.getHelpText());
        Assertions.assertEquals(2, dummyConfiguredProvider.getProperties().size());
        Assertions.assertProviderConfigProperty(dummyConfiguredProvider.getProperties().get(0), "prop1", "Prop1", "prop1Default", "Prop1 HelpText", ProviderConfigProperty.STRING_TYPE);
        Assertions.assertProviderConfigProperty(dummyConfiguredProvider.getProperties().get(1), "prop2", "Prop2", "true", "Prop2 HelpText", ProviderConfigProperty.BOOLEAN_TYPE);

        try {
            userFederation().getProviderFactory("not-existent");
            Assertions.fail("Not expected to find not-existent provider");
        } catch (NotFoundException nfe) {
            // Expected
        }
    }

    private UserFederationProvidersResource userFederation() {
        return null;//realm.userFederation();
    }


    @Test
    public void testCreateProvider() {
        // create provider without configuration and displayName
        UserFederationProviderRepresentation dummyRep1 = UserFederationProviderBuilder.create()
                .providerName("dummy")
                .displayName("")
                .priority(2)
                .fullSyncPeriod(1000)
                .changedSyncPeriod(500)
                .lastSync(123)
                .build();

        String id1 = createUserFederationProvider(dummyRep1);

        // create provider with configuration and displayName
        UserFederationProviderRepresentation dummyRep2 = UserFederationProviderBuilder.create()
                .providerName("dummy")
                .displayName("dn1")
                .priority(1)
                .configProperty("prop1", "prop1Val")
                .configProperty("prop2", "true")
                .build();
        String id2 = createUserFederationProvider(dummyRep2);

        // Assert provider instances available
        assertFederationProvider(userFederation().get(id1).toBriefRepresentation(), id1, id1, "dummy", 2, 1000, 500, 123);
        assertFederationProvider(userFederation().get(id2).toBriefRepresentation(), id2, "dn1", "dummy", 1, -1, -1, -1, "prop1", "prop1Val", "prop2", "true");

        // Assert sorted
        List<UserFederationProviderRepresentation> providerInstances = userFederation().getProviderInstances();
        Assertions.assertEquals(providerInstances.size(), 2);
        assertFederationProvider(providerInstances.get(0), id2, "dn1", "dummy", 1, -1, -1, -1, "prop1", "prop1Val", "prop2", "true");
        assertFederationProvider(providerInstances.get(1), id1, id1, "dummy", 2, 1000, 500, 123);

        // Remove providers
        removeUserFederationProvider(id1);
        removeUserFederationProvider(id2);
    }








    @Test (expected = NotFoundException.class)
    public void testLookupNotExistentProvider() {
        userFederation().get("not-existent").toBriefRepresentation();
    }


    @Test
    public void testSyncFederationProvider() {
        // create provider
        UserFederationProviderRepresentation dummyRep1 = UserFederationProviderBuilder.create()
                .providerName("dummy")
                .build();
        String id1 = createUserFederationProvider(dummyRep1);


        // Sync with unknown action shouldn't pass
        try {
            userFederation().get(id1).syncUsers("unknown");
            Assertions.fail("Not expected to sync with unknown action");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Assert sync didn't happen
        Assertions.assertEquals(-1, userFederation().get(id1).toBriefRepresentation().getLastSync());

        // Sync and assert it happened
        SynchronizationResultRepresentation syncResult = userFederation().get(id1).syncUsers("triggerFullSync");
        Assertions.assertEquals("0 imported users, 0 updated users", syncResult.getStatus());

        Map<String, Object> eventRep = new HashMap<>();
        eventRep.put("action", "triggerFullSync");
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, AdminEventPaths.userFederationResourcePath(id1) + "/sync", eventRep, ResourceType.USER_FEDERATION_PROVIDER);

        int fullSyncTime = userFederation().get(id1).toBriefRepresentation().getLastSync();
        Assertions.assertTrue(fullSyncTime > 0);

        // Changed sync
        setTimeOffset(50);
        syncResult = userFederation().get(id1).syncUsers("triggerChangedUsersSync");

        eventRep.put("action", "triggerChangedUsersSync");
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, AdminEventPaths.userFederationResourcePath(id1) + "/sync", eventRep, ResourceType.USER_FEDERATION_PROVIDER);

        Assertions.assertEquals("0 imported users, 0 updated users", syncResult.getStatus());
        int changedSyncTime = userFederation().get(id1).toBriefRepresentation().getLastSync();
        Assertions.assertTrue(fullSyncTime + 50 <= changedSyncTime);

        // Cleanup
        resetTimeOffset();
        removeUserFederationProvider(id1);
    }



    private void assertFederationProvider(UserFederationProviderRepresentation rep, String id, String displayName, String providerName,
                                          int priority, int fullSyncPeriod, int changeSyncPeriod, int lastSync,
                                          String... config) {
        Assertions.assertEquals(id, rep.getId());
        Assertions.assertEquals(displayName, rep.getDisplayName());
        Assertions.assertEquals(providerName, rep.getProviderName());
        Assertions.assertEquals(priority, rep.getPriority());
        Assertions.assertEquals(fullSyncPeriod, rep.getFullSyncPeriod());
        Assertions.assertEquals(changeSyncPeriod, rep.getChangedSyncPeriod());
        Assertions.assertEquals(lastSync, rep.getLastSync());

        Assertions.assertMap(rep.getConfig(), config);
    }


    */
}
