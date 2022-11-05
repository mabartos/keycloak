package org.keycloak.reactive.models.jpa;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.async.UserAsyncProvider;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserRoleMappingEntity;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class JpaUserAsyncProvider implements UserAsyncProvider {

    private final KeycloakSession session;
    private final Mutiny.SessionFactory sf;

    public JpaUserAsyncProvider(KeycloakSession session, Mutiny.SessionFactory sessionFactory) {
        this.session = session;
        this.sf = sessionFactory;
    }

    @Override
    public void close() {

    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {
        if (realm.equals(role.isClientRole() ? ((ClientModel) role.getContainer()).getRealm() : role.getContainer())) {
            sf.withTransaction(session -> session.createNamedQuery("grantRoleToAllUsers", UserRoleMappingEntity.class)
                    .setParameter("realmId", realm.getId())
                    .setParameter("roleId", role.getId())
                    .executeUpdate());
        }
    }

    @Override
    public Uni<UserModel> getUserById(RealmModel realm, String id) {
        return sf.withSession(s -> {
            Uni<UserEntity> userEntity = s.find(UserEntity.class, id);
            return userEntity.onItem()
                    .ifNotNull()
                    .call(f -> {
                        if (!realm.getId().equals(f.getRealmId())) return Uni.createFrom().nullItem();
                        return Uni.createFrom().item(f);
                    })
                    .onItem()
                    //TODO use Mutiny Session
                    .transform(f -> new UserAdapter(session, realm, null, f));
        });
    }

    @Override
    public Uni<UserModel> getUserByUsername(RealmModel realm, String username) {
        return null;
    }

    @Override
    public Uni<UserModel> getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    @Override
    public Multi<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        return null;
    }

    @Override
    public Multi<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        return null;
    }

    @Override
    public Multi<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return null;
    }

    @Override
    public Multi<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return null;
    }

    @Override
    public Uni<UserModel> addUser(RealmModel realm, String username) {
        return null;
    }

    @Override
    public Uni<Boolean> removeUser(RealmModel realm, UserModel user) {
        return null;
    }
}
