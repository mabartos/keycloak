package org.keycloak.authentication.adaptive.factors.role;

import org.keycloak.models.RoleModel;
import org.keycloak.adaptive.factors.UserContext;

import java.util.Set;

public interface UserRoleContext extends UserContext<Set<RoleModel>> {
}
